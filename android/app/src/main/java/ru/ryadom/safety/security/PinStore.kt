package ru.ryadom.safety.security

import android.content.Context
import java.security.MessageDigest

object PinStore {
    private const val PREFS = "ryadom_parent_security"
    private const val KEY = "pin_hash"

    fun hasPin(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(KEY)

    fun set(context: Context, pin: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, hash(pin)).apply()
    }

    fun verify(context: Context, pin: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "") == hash(pin)

    private fun hash(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
