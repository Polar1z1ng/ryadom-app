package ru.ryadom.safety.telegram

import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import ru.ryadom.safety.rules.RiskEngine
import ru.ryadom.safety.storage.AlertEvent
import ru.ryadom.safety.storage.AlertStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object TelegramCore {
    private val mutableState = MutableStateFlow<TelegramState>(TelegramState.Starting)
    val state: StateFlow<TelegramState> = mutableState.asStateFlow()

    private var appContext: Context? = null
    private var client: Client? = null
    private var started = false
    private var currentAuthorizationState: TdApi.AuthorizationState? = null
    private val chatTitles = ConcurrentHashMap<Long, String>()

    @Synchronized
    fun ensureStarted(context: Context) {
        if (started) return
        started = true
        appContext = context.applicationContext

        try {
            System.loadLibrary("tdjni")
            client = Client.create(
                Client.ResultHandler { obj -> handle(obj) },
                Client.ExceptionHandler { error ->
                    mutableState.value = TelegramState.Error(error.message ?: "Ошибка TDLib")
                },
                Client.ExceptionHandler { error ->
                    mutableState.value = TelegramState.Error(error.message ?: "Ошибка TDLib")
                }
            )
        } catch (t: Throwable) {
            mutableState.value = TelegramState.Error("Не удалось запустить Telegram: " + (t.message ?: t.javaClass.simpleName))
        }
    }

    fun configure(context: Context, apiId: Int, apiHash: String) {
        SecretStore.saveTelegramCredentials(context, apiId, apiHash.trim())
        ensureStarted(context)
        val auth = currentAuthorizationState
        if (auth is TdApi.AuthorizationStateWaitTdlibParameters) {
            sendTdlibParameters()
        } else {
            mutableState.value = TelegramState.Starting
        }
        startMonitorService(context)
    }

    fun submitPhone(phone: String) =
        send(TdApi.SetAuthenticationPhoneNumber(phone.trim(), null))

    fun submitCode(code: String) =
        send(TdApi.CheckAuthenticationCode(code.trim()))

    fun submitPassword(password: String) =
        send(TdApi.CheckAuthenticationPassword(password))

    fun submitEmail(email: String) =
        send(TdApi.SetAuthenticationEmailAddress(email.trim()))

    fun submitEmailCode(code: String) =
        send(TdApi.CheckAuthenticationEmailCode(TdApi.EmailAddressAuthenticationCode(code.trim())))

    fun logout(context: Context) {
        send(TdApi.LogOut())
        SecretStore.clearTelegramCredentials(context)
    }

    private fun handle(obj: TdApi.Object) {
        when (obj) {
            is TdApi.UpdateAuthorizationState -> handleAuthorization(obj.authorizationState)
            is TdApi.UpdateNewChat -> chatTitles[obj.chat.id] = obj.chat.title
            is TdApi.UpdateChatTitle -> chatTitles[obj.chatId] = obj.title
            is TdApi.UpdateNewMessage -> analyzeMessage(obj.message.chatId, obj.message.content)
            is TdApi.UpdateMessageContent -> analyzeMessage(obj.chatId, obj.newContent)
        }
    }

    private fun handleAuthorization(auth: TdApi.AuthorizationState) {
        currentAuthorizationState = auth
        val context = appContext ?: return

        when (auth) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                if (SecretStore.hasTelegramCredentials(context)) sendTdlibParameters()
                else mutableState.value = TelegramState.NeedCredentials
            }
            is TdApi.AuthorizationStateWaitPhoneNumber -> mutableState.value = TelegramState.NeedPhone
            is TdApi.AuthorizationStateWaitCode -> mutableState.value = TelegramState.NeedCode
            is TdApi.AuthorizationStateWaitPassword -> mutableState.value = TelegramState.NeedPassword(auth.passwordHint ?: "")
            is TdApi.AuthorizationStateWaitEmailAddress -> mutableState.value = TelegramState.NeedEmail
            is TdApi.AuthorizationStateWaitEmailCode -> mutableState.value = TelegramState.NeedEmailCode
            is TdApi.AuthorizationStateWaitOtherDeviceConfirmation -> mutableState.value = TelegramState.ConfirmOnOtherDevice(auth.link)
            is TdApi.AuthorizationStateReady -> {
                mutableState.value = TelegramState.Ready
                startMonitorService(context)
            }
            is TdApi.AuthorizationStateLoggingOut,
            is TdApi.AuthorizationStateClosing,
            is TdApi.AuthorizationStateClosed -> mutableState.value = TelegramState.Starting
        }
    }

    private fun sendTdlibParameters() {
        val context = appContext ?: return
        val apiId = SecretStore.telegramApiId(context)
        val apiHash = SecretStore.telegramApiHash(context)
        if (apiId == null || apiHash.isNullOrBlank()) {
            mutableState.value = TelegramState.NeedCredentials
            return
        }

        val request = TdApi.SetTdlibParameters()
        request.databaseDirectory = context.filesDir.absolutePath + "/telegram-db"
        request.useMessageDatabase = true
        request.useSecretChats = false
        request.apiId = apiId
        request.apiHash = apiHash
        request.systemLanguageCode = Locale.getDefault().toLanguageTag()
        request.deviceModel = Build.MANUFACTURER + " " + Build.MODEL
        request.applicationVersion = "Ryadom 0.3"

        send(request)
    }

    private fun send(function: TdApi.Function<*>) {
        client?.send(function, Client.ResultHandler { result ->
            if (result is TdApi.Error) {
                mutableState.value = TelegramState.Error(result.message ?: "Ошибка Telegram")
            }
        })
    }

    private fun analyzeMessage(chatId: Long, content: TdApi.MessageContent) {
        val context = appContext ?: return
        val text = extractText(content) ?: return
        if (text.isBlank()) return

        val result = RiskEngine.analyze(text)
        if (result.first < 20) return

        AlertStore.add(
            context,
            AlertEvent(
                time = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date()),
                source = "Telegram",
                chat = chatTitles[chatId] ?: "Чат Telegram",
                score = result.first,
                categories = result.second.joinToString(", "),
                text = text.take(240)
            )
        )
    }

    private fun extractText(content: TdApi.MessageContent): String? = when (content) {
        is TdApi.MessageText -> content.text.text
        is TdApi.MessagePhoto -> content.caption.text
        is TdApi.MessageVideo -> content.caption.text
        is TdApi.MessageDocument -> content.caption.text
        is TdApi.MessageAnimation -> content.caption.text
        else -> null
    }

    private fun startMonitorService(context: Context) {
        runCatching {
            context.startForegroundService(Intent(context, TelegramMonitorService::class.java))
        }
    }
}
