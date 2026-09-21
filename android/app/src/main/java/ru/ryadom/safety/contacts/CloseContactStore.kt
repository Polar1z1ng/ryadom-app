package ru.ryadom.safety.contacts

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

object CloseContactStore {
    private const val PREFS = "ryadom_close_contact"
    private const val KEY_NUMBER = "phone"

    fun number(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_NUMBER, "")
            .orEmpty()

    fun save(context: Context, raw: String) {
        val normalized = normalize(raw)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NUMBER, normalized)
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_NUMBER)
            .apply()
    }

    fun hasCallPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

    fun call(context: Context): Boolean {
        val phone = number(context)
        if (phone.isBlank() || !hasCallPermission(context)) return false

        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        return true
    }

    private fun normalize(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""
        val plus = trimmed.startsWith("+")
        val digits = trimmed.filter(Char::isDigit)
        return if (plus) "+$digits" else digits
    }
}
