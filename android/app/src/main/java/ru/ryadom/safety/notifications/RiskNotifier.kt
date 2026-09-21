package ru.ryadom.safety.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.net.Uri
import ru.ryadom.safety.MainActivity
import ru.ryadom.safety.contacts.CloseContactStore
import ru.ryadom.safety.R
import ru.ryadom.safety.storage.AlertEvent
import java.util.concurrent.atomic.AtomicInteger

object RiskNotifier {
    private const val CHANNEL_RISK = "ryadom_risk_alerts"
    private const val CHANNEL_CRITICAL = "ryadom_critical_alerts"
    private val ids = AtomicInteger(4100)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun notify(context: Context, event: AlertEvent) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        ensureChannels(context)
        post(context, event, repeatIndex = 0)

        when {
            event.score >= 90 -> {
                mainHandler.postDelayed({ post(context.applicationContext, event, 1) }, 15_000L)
                mainHandler.postDelayed({ post(context.applicationContext, event, 2) }, 45_000L)
            }
            event.score >= 70 -> {
                mainHandler.postDelayed({ post(context.applicationContext, event, 1) }, 20_000L)
            }
        }
    }

    private fun post(context: Context, event: AlertEvent, repeatIndex: Int) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = context.getSystemService(NotificationManager::class.java)
        val critical = event.score >= 70
        val channel = if (critical) CHANNEL_CRITICAL else CHANNEL_RISK

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_events", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            ids.incrementAndGet(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when {
            event.score >= 90 && repeatIndex > 0 -> "Повторное предупреждение · высокий риск"
            event.score >= 90 -> "Высокий риск · Рядом"
            event.score >= 70 && repeatIndex > 0 -> "Повторное предупреждение · Рядом"
            event.score >= 70 -> "Тревожный сигнал · Рядом"
            else -> "Рядом заметил риск"
        }

        val source = event.source.ifBlank { "Источник" }
        val category = event.categories.ifBlank { "Риск-событие" }
        val body = "$source · $category\n${event.text.take(140)}"

        val builder = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(context, channel)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
                .setPriority(if (critical) Notification.PRIORITY_MAX else Notification.PRIORITY_HIGH)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(if (critical) longArrayOf(0, 650, 250, 650) else longArrayOf(0, 350))
        }

        builder
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(false)

        val closeNumber = CloseContactStore.number(context)
        if (closeNumber.isNotBlank() && CloseContactStore.hasCallPermission(context)) {
            val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$closeNumber"))
            val callPendingIntent = PendingIntent.getActivity(
                context,
                ids.incrementAndGet(),
                callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                Notification.Action.Builder(
                    R.drawable.ic_notification,
                    "Позвонить близкому",
                    callPendingIntent
                ).build()
            )
        }

        if (Build.VERSION.SDK_INT >= 21) {
            builder.setColor(0xFFD95A43.toInt())
        }

        manager.notify(ids.incrementAndGet(), builder.build())
    }

    private fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return

        val manager = context.getSystemService(NotificationManager::class.java)
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val normal = NotificationChannel(
            CHANNEL_RISK,
            "Рядом · предупреждения",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Предупреждения о риск-событиях"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 350, 180, 350)
            setSound(sound, audio)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }

        val critical = NotificationChannel(
            CHANNEL_CRITICAL,
            "Рядом · высокий риск",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Срочные предупреждения о высоком риске"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 650, 250, 650, 250, 650)
            setSound(sound, audio)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }

        manager.createNotificationChannel(normal)
        manager.createNotificationChannel(critical)
    }
}
