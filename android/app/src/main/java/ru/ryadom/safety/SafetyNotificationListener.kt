package ru.ryadom.safety

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SafetyNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        getSharedPreferences("ryadom_diagnostics", MODE_PRIVATE).edit()
            .putBoolean("listener_connected", true)
            .putString("listener_state_time", now())
            .apply()
    }

    override fun onListenerDisconnected() {
        getSharedPreferences("ryadom_diagnostics", MODE_PRIVATE).edit()
            .putBoolean("listener_connected", false)
            .putString("listener_state_time", now())
            .apply()
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return

        // Keep only a package-name diagnostic for any notification.
        // This helps diagnose Telegram/VK variants without storing unrelated message text.
        getSharedPreferences("ryadom_diagnostics", MODE_PRIVATE).edit()
            .putString("last_package_any", pkg)
            .putString("last_package_any_time", now())
            .apply()

        val source = sourceFor(pkg) ?: return
        val message = extractMessage(sbn.notification.extras)

        val diag = getSharedPreferences("ryadom_diagnostics", MODE_PRIVATE)
        diag.edit()
            .putString("last_source", source)
            .putString("last_package", pkg)
            .putString("last_text", if (message.isBlank()) "(Android не передал текст уведомления)" else message.take(300))
            .putString("last_time", now())
            .apply()

        if (message.isBlank()) return

        val (score, categories) = RiskEngine.analyze(message)
        if (score < 20) return

        val entry = "${now()} • $source • $score/100\n${categories.joinToString(", ")}\n${message.take(180)}"
        val p = getSharedPreferences("ryadom_alerts", MODE_PRIVATE)
        val old = p.getString("log", "").orEmpty()
        p.edit().putString("log", if (old.isBlank()) entry else "$entry\n\n$old").apply()
    }

    private fun sourceFor(pkg: String): String? = when {
        pkg.startsWith("org.telegram.messenger") -> "Telegram"
        pkg == "org.thunderdog.challegram" -> "Telegram X"
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
            val b = item as? Bundle
            add(b?.getCharSequence("text"))
        }

        return parts.joinToString("\n")
    }

    private fun now(): String =
        SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault()).format(Date())
}
