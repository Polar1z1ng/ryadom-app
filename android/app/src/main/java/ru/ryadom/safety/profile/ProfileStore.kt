package ru.ryadom.safety.profile

import android.content.Context

data class UserProfile(
    val fullName: String = "",
    val birthDate: String = ""
)

object ProfileStore {
    private const val PREFS = "ryadom_profile"

    fun read(context: Context): UserProfile {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return UserProfile(
            fullName = p.getString("full_name", "").orEmpty(),
            birthDate = p.getString("birth_date", "").orEmpty()
        )
    }

    fun save(context: Context, profile: UserProfile) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString("full_name", profile.fullName.trim())
            .putString("birth_date", profile.birthDate.trim())
            .apply()
    }
}
