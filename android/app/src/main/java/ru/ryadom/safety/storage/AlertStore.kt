package ru.ryadom.safety.storage

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import ru.ryadom.safety.notifications.RiskNotifier

data class AlertEvent(
    val time: String,
    val source: String,
    val chat: String,
    val score: Int,
    val categories: String,
    val text: String,
    val id: Long = 0L,
    val acknowledged: Boolean = false
)

object AlertStore {
    private const val PREFS = "ryadom_alert_events"
    private const val KEY = "events"

    @Synchronized
    fun add(context: Context, event: AlertEvent) {
        val stored = if (event.id == 0L) {
            event.copy(id = System.currentTimeMillis(), acknowledged = false)
        } else {
            event.copy(acknowledged = false)
        }

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = JSONArray(prefs.getString(KEY, "[]"))
        val out = JSONArray()
        out.put(toJson(stored))
        for (i in 0 until minOf(current.length(), 49)) out.put(current.get(i))
        prefs.edit().putString(KEY, out.toString()).apply()

        RiskNotifier.notify(context.applicationContext, stored)
    }

    fun read(context: Context): List<AlertEvent> {
        return runCatching {
            val array = JSONArray(
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]")
            )
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    val id = o.optLong("id", 0L)
                    add(
                        AlertEvent(
                            time = o.optString("time"),
                            source = o.optString("source"),
                            chat = o.optString("chat"),
                            score = o.optInt("score"),
                            categories = o.optString("categories"),
                            text = o.optString("text"),
                            id = id,
                            acknowledged = if (o.has("acknowledged")) {
                                o.optBoolean("acknowledged", false)
                            } else {
                                // Старые события до появления индикатора не считаем непрочитанными.
                                true
                            }
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun markAcknowledged(context: Context, eventId: Long) {
        if (eventId == 0L) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = JSONArray(prefs.getString(KEY, "[]"))
        val out = JSONArray()

        for (i in 0 until current.length()) {
            val item = current.getJSONObject(i)
            if (item.optLong("id", 0L) == eventId) {
                item.put("acknowledged", true)
            }
            out.put(item)
        }
        prefs.edit().putString(KEY, out.toString()).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }

    private fun toJson(event: AlertEvent) = JSONObject()
        .put("time", event.time)
        .put("source", event.source)
        .put("chat", event.chat)
        .put("score", event.score)
        .put("categories", event.categories)
        .put("text", event.text)
        .put("id", event.id)
        .put("acknowledged", event.acknowledged)
}
