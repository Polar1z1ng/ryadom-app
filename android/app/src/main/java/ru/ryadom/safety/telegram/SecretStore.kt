package ru.ryadom.safety.telegram

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object SecretStore {
    private const val KEY_ALIAS = "ryadom_secrets_v1"
    private const val PREFS = "ryadom_secure"

    fun saveTelegramCredentials(context: Context, apiId: Int, apiHash: String) {
        put(context, "telegram_api_id", apiId.toString())
        put(context, "telegram_api_hash", apiHash)
    }

    fun telegramApiId(context: Context): Int? =
        get(context, "telegram_api_id")?.toIntOrNull()

    fun telegramApiHash(context: Context): String? =
        get(context, "telegram_api_hash")

    fun hasTelegramCredentials(context: Context): Boolean =
        telegramApiId(context) != null && !telegramApiHash(context).isNullOrBlank()

    fun clearTelegramCredentials(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    private fun put(context: Context, name: String, value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val packed = Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(name, packed).apply()
    }

    private fun get(context: Context, name: String): String? {
        val packed = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(name, null) ?: return null
        return runCatching {
            val raw = Base64.decode(packed, Base64.NO_WRAP)
            val iv = raw.copyOfRange(0, 12)
            val encrypted = raw.copyOfRange(12, raw.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.getOrNull()
    }
}
