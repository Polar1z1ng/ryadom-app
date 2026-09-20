package ru.ryadom.safety.vk

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import ru.ryadom.safety.RiskEngine
import ru.ryadom.safety.storage.AlertEvent
import ru.ryadom.safety.storage.AlertStore
import ru.ryadom.safety.telegram.SecretStore
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VkCore {
    private val mutableState = MutableStateFlow<VkState>(VkState.Disconnected)
    val state: StateFlow<VkState> = mutableState.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollJob: Job? = null
    private var appContext: Context? = null

    fun ensureStarted(context: Context) {
        appContext = context.applicationContext
        val token = SecretStore.vkToken(context)
        if (!token.isNullOrBlank() && pollJob?.isActive != true) {
            startPolling(context.applicationContext, token)
        }
    }

    fun connect(context: Context, token: String) {
        val clean = token.trim()
        if (clean.isBlank()) {
            mutableState.value = VkState.Error("Пустой токен VK")
            return
        }
        SecretStore.saveVkToken(context, clean)
        appContext = context.applicationContext
        startMonitorService(context)
        startPolling(context.applicationContext, clean)
    }

    fun disconnect(context: Context) {
        pollJob?.cancel()
        pollJob = null
        SecretStore.clearVkToken(context)
        mutableState.value = VkState.Disconnected
        context.stopService(Intent(context, VkMonitorService::class.java))
    }

    private fun startPolling(context: Context, token: String) {
        pollJob?.cancel()
        pollJob = scope.launch {
            var firstSuccessfulCycle = true
            while (isActive) {
                try {
                    mutableState.value = VkState.Connecting
                    val root = api(
                        token,
                        "messages.getConversations",
                        mapOf(
                            "count" to "50",
                            "extended" to "0"
                        )
                    )
                    val response = root.optJSONObject("response")
                        ?: throw IllegalStateException(vkError(root))
                    val items = response.optJSONArray("items") ?: org.json.JSONArray()
                    val prefs = context.getSharedPreferences("ryadom_vk_direct", Context.MODE_PRIVATE)

                    for (i in 0 until items.length()) {
                        val item = items.optJSONObject(i) ?: continue
                        val last = item.optJSONObject("last_message") ?: continue
                        val peerId = last.optLong("peer_id", 0L)
                        val lastId = last.optLong("id", 0L)
                        if (peerId == 0L || lastId == 0L) continue

                        val key = "peer_" + peerId
                        val seen = prefs.getLong(key, 0L)

                        if (seen == 0L) {
                            prefs.edit().putLong(key, lastId).apply()
                            continue
                        }

                        if (lastId > seen) {
                            val historyRoot = api(
                                token,
                                "messages.getHistory",
                                mapOf(
                                    "peer_id" to peerId.toString(),
                                    "count" to "30",
                                    "rev" to "0"
                                )
                            )
                            val history = historyRoot.optJSONObject("response")
                                ?: throw IllegalStateException(vkError(historyRoot))
                            val messages = history.optJSONArray("items") ?: org.json.JSONArray()
                            var highest = seen

                            val pending = mutableListOf<JSONObject>()
                            for (j in 0 until messages.length()) {
                                val message = messages.optJSONObject(j) ?: continue
                                val id = message.optLong("id", 0L)
                                if (id > seen) pending += message
                                highest = maxOf(highest, id)
                            }

                            pending.sortedBy { it.optLong("id") }.forEach { message ->
                                if (message.optInt("out", 1) != 0) return@forEach
                                val text = message.optString("text").trim()
                                if (text.isBlank()) return@forEach
                                analyze(context, peerId, text, message.optLong("date", 0L))
                            }

                            prefs.edit().putLong(key, maxOf(highest, lastId)).apply()
                        }
                    }

                    firstSuccessfulCycle = false
                    mutableState.value = VkState.Ready
                    delay(4500)
                } catch (t: Throwable) {
                    mutableState.value = VkState.Error(
                        t.message?.take(180) ?: "Ошибка подключения VK"
                    )
                    delay(if (firstSuccessfulCycle) 8000 else 15000)
                }
            }
        }
    }

    private fun analyze(context: Context, peerId: Long, text: String, unixSeconds: Long) {
        val result = RiskEngine.analyze(text)
        if (result.first < 20) return

        val whenMs = if (unixSeconds > 0L) unixSeconds * 1000L else System.currentTimeMillis()
        AlertStore.add(
            context,
            AlertEvent(
                time = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(whenMs)),
                source = "VK",
                chat = "Диалог " + peerId,
                score = result.first,
                categories = result.second.joinToString(", "),
                text = text.take(240)
            )
        )
    }

    private fun api(token: String, method: String, params: Map<String, String>): JSONObject {
        val url = URL("https://api.vk.com/method/" + method)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 30000
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
        }

        val body = buildList {
            add("access_token=" + enc(token))
            add("v=5.199")
            params.forEach { (k, v) -> add(enc(k) + "=" + enc(v)) }
        }.joinToString("&")

        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val text = stream.bufferedReader().use { it.readText() }
        connection.disconnect()
        return JSONObject(text)
    }

    private fun vkError(root: JSONObject): String {
        val error = root.optJSONObject("error") ?: return "VK вернул пустой ответ"
        val message = error.optString("error_msg", "Ошибка VK API")
        val code = error.optInt("error_code", 0)
        return if (code == 0) message else "VK " + code + ": " + message
    }

    private fun enc(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun startMonitorService(context: Context) {
        runCatching {
            context.startForegroundService(Intent(context, VkMonitorService::class.java))
        }
    }
}
