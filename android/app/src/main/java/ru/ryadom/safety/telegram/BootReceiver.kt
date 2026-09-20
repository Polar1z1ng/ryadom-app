package ru.ryadom.safety.telegram

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ru.ryadom.safety.vk.VkMonitorService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        if (SecretStore.hasTelegramCredentials(context)) {
            runCatching {
                context.startForegroundService(Intent(context, TelegramMonitorService::class.java))
            }
        }

        if (SecretStore.hasVkToken(context)) {
            runCatching {
                context.startForegroundService(Intent(context, VkMonitorService::class.java))
            }
        }
    }
}
