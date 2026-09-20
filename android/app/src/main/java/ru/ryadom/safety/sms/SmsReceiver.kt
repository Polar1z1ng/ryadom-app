package ru.ryadom.safety.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import ru.ryadom.safety.RiskEngine
import ru.ryadom.safety.storage.AlertEvent
import ru.ryadom.safety.storage.AlertStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return

        val sender = messages.firstOrNull()?.originatingAddress.orEmpty().ifBlank { "Неизвестный номер" }
        val text = messages.joinToString("") { it.messageBody.orEmpty() }.trim()
        if (text.isBlank()) return

        val result = RiskEngine.analyze(text)
        if (result.first < 20) return

        AlertStore.add(
            context,
            AlertEvent(
                time = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date()),
                source = "SMS",
                chat = sender,
                score = result.first,
                categories = result.second.joinToString(", "),
                text = text.take(240)
            )
        )
    }
}
