package ru.ryadom.safety.telegram

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED && SecretStore.hasTelegramCredentials(context)) {
            runCatching {
                context.startForegroundService(Intent(context, TelegramMonitorService::class.java))
            }
        }
    }
}
