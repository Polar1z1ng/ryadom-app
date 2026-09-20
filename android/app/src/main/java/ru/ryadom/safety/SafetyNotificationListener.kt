package ru.ryadom.safety

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import ru.ryadom.safety.storage.AlertEvent
import ru.ryadom.safety.storage.AlertStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SafetyNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val source = sourceFor(sbn.packageName ?: return) ?: return
        val message = extractMessage(sbn.notification.extras)
        if (message.isBlank()) return

        val result = RiskEngine.analyze(message)
        if (result.first < 20) return

        AlertStore.add(
            this,
            AlertEvent(
                time = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date()),
                source = source,
                chat = "Уведомление",
                score = result.first,
                categories = result.second.joinToString(", "),
                text = message.take(240)
            )
        )
    }

    private fun sourceFor(pkg: String): String? = when {
        pkg.startsWith("com.vkontakte.android") -> "VK"
        pkg == "com.google.android.apps.messaging" -> "SMS"
        pkg == "com.samsung.android.messaging" -> "SMS"
        pkg == "com.android.mms" -> "SMS"
        pkg == "com.miui.mms" -> "SMS"
        else -> null
    }

    private fun extractMessage(e: Bundle): String {
        val parts = mutableListOf<String>()

        fun add(value: CharSequence?) {
            val s = value?.toString()?.trim().orEmpty()
            if (s.isNotBlank() && s !in parts) parts += s
        }

        add(e.getCharSequence(Notification.EXTRA_TITLE))
        add(e.getCharSequence(Notification.EXTRA_TEXT))
        add(e.getCharSequence(Notification.EXTRA_BIG_TEXT))
        e.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach { add(it) }

        e.getParcelableArray(Notification.EXTRA_MESSAGES)?.forEach { item ->
            val bundle = item as? Bundle
            add(bundle?.getCharSequence("text"))
        }

        return parts.joinToString("\n")
    }
}
