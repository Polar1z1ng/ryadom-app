package ru.ryadom.safety.family

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class FamilyMember(
    val id: String,
    val name: String,
    val role: String,
    val phone: String = "",
    val protected: Boolean = true
)

object FamilyStore {
    private const val PREFS = "ryadom_family"
    private const val KEY = "members"

    fun read(context: Context): List<FamilyMember> {
        return runCatching {
            val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY, "[]")
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        FamilyMember(
                            id = o.optString("id"),
                            name = o.optString("name"),
                            role = o.optString("role"),
                            phone = o.optString("phone"),
                            protected = o.optBoolean("protected", true)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun add(context: Context, name: String, role: String, phone: String) {
        val list = read(context).toMutableList()
        list.add(
            FamilyMember(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                role = role.trim(),
                phone = normalizePhone(phone),
                protected = true
            )
        )
        save(context, list)
    }

    fun update(context: Context, member: FamilyMember) {
        val list = read(context).map { if (it.id == member.id) member else it }
        save(context, list)
    }

    fun remove(context: Context, id: String) {
        save(context, read(context).filterNot { it.id == id })
    }

    private fun save(context: Context, list: List<FamilyMember>) {
        val array = JSONArray()
        list.forEach { member ->
            array.put(
                JSONObject()
                    .put("id", member.id)
                    .put("name", member.name)
                    .put("role", member.role)
                    .put("phone", member.phone)
                    .put("protected", member.protected)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, array.toString())
            .apply()
    }

    private fun normalizePhone(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""
        val plus = trimmed.startsWith("+")
        val digits = trimmed.filter(Char::isDigit)
        return if (plus) "+$digits" else digits
    }
}
