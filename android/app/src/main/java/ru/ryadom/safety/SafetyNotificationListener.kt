package ru.ryadom.safety

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SafetyNotificationListener : NotificationListenerService() {
    private val monitored = setOf(
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "com.vkontakte.android",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in monitored) return
        val e = sbn.notification.extras
        val title = e.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = e.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val big = e.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val lines = e.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString(" ") { it.toString() }.orEmpty()
        val message = listOf(title,text,big,lines).filter { it.isNotBlank() }.distinct().joinToString("\n")
        if (message.isBlank()) return

        val (score,categories) = RiskEngine.analyze(message)
        if (score < 20) return

        val source = when (sbn.packageName) {
            "org.telegram.messenger","org.telegram.messenger.web" -> "Telegram"
            "com.vkontakte.android" -> "VK"
            else -> "SMS"
        }
        val time = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date())
        val entry = "$time • $source • $score/100\n${categories.joinToString(", ")}\n${message.take(180)}"
        val p = getSharedPreferences("ryadom_alerts", MODE_PRIVATE)
        val old = p.getString("log", "").orEmpty()
        p.edit().putString("log", if (old.isBlank()) entry else "$entry\n\n$old").apply()
    }
}
