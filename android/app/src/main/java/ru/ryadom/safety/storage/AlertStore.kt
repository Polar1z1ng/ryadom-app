package ru.ryadom.safety.storage

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AlertEvent(
    val time: String,
    val source: String,
    val chat: String,
    val score: Int,
    val categories: String,
    val text: String
)

object AlertStore {
    private const val PREFS = "ryadom_alert_events"
    private const val KEY = "events"

    fun add(context: Context, event: AlertEvent) {
        val current = JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]"))
        val out = JSONArray()
        out.put(toJson(event))
        for (i in 0 until minOf(current.length(), 49)) out.put(current.get(i))
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, out.toString()).apply()
    }

    fun read(context: Context): List<AlertEvent> {
        return runCatching {
            val array = JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]"))
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        AlertEvent(
                            time = o.optString("time"),
                            source = o.optString("source"),
                            chat = o.optString("chat"),
                            score = o.optInt("score"),
                            categories = o.optString("categories"),
                            text = o.optString("text")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }

    private fun toJson(event: AlertEvent) = JSONObject()
        .put("time", event.time)
        .put("source", event.source)
        .put("chat", event.chat)
        .put("score", event.score)
        .put("categories", event.categories)
        .put("text", event.text)
}
