package ru.ryadom.safety.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import ru.ryadom.safety.RiskEngine
import ru.ryadom.safety.storage.AlertEvent
import ru.ryadom.safety.storage.AlertStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SmsDirect {
    private const val PREFS = "ryadom_sms_direct"
    private const val LAST_ID = "last_seen_id"

    fun hasPermissions(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

    fun syncRecent(context: Context, maxRows: Int = 80) {
        if (!hasPermissions(context)) return

        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastSeen = prefs.getLong(LAST_ID, 0L)
        var highestSeen = lastSeen
        var scanned = 0

        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )

        context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            null,
            null,
            Telephony.Sms.DEFAULT_SORT_ORDER
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressCol = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyCol = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateCol = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)

            while (cursor.moveToNext() && scanned < maxRows) {
                scanned++
                val id = cursor.getLong(idCol)
                highestSeen = maxOf(highestSeen, id)
                if (lastSeen != 0L && id <= lastSeen) continue

                val sender = cursor.getString(addressCol).orEmpty().ifBlank { "Неизвестный номер" }
                val body = cursor.getString(bodyCol).orEmpty().trim()
                if (body.isBlank()) continue

                val result = RiskEngine.analyze(body)
                if (result.first < 20) continue

                val date = cursor.getLong(dateCol)
                AlertStore.add(
                    context,
                    AlertEvent(
                        time = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(date)),
                        source = "SMS",
                        chat = sender,
                        score = result.first,
                        categories = result.second.joinToString(", "),
                        text = body.take(240)
                    )
                )
            }
        }

        if (highestSeen > 0L) {
            prefs.edit().putLong(LAST_ID, highestSeen).apply()
        }
    }
}
