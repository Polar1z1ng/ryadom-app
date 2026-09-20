package ru.ryadom.safety

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.*
import java.security.MessageDigest

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var journal: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        }
        setContentView(ui())
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun ui(): ScrollView {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(24), dp(20), dp(40))
        }
        scroll.addView(root)
        root.addView(TextView(this).apply {
            text = "Рядом"
            textSize = 30f
            setTypeface(typeface, Typeface.BOLD)
        })
        root.addView(TextView(this).apply {
            text = "Тестовая защита Telegram, VK и SMS"
            textSize = 16f
        })
        root.addView(TextView(this).apply {
            text = "Приложение не скрывается. Пользователь знает, что опасные сообщения анализируются локально."
            textSize = 14f
            setPadding(0, dp(8), 0, dp(16))
        })

        root.addView(header("1. PIN родителя"))
        root.addView(Button(this).apply {
            text = if (hasPin()) "Изменить PIN" else "Установить PIN"
            setOnClickListener { withPin { askNewPin() } }
        })

        root.addView(header("2. Доступ к уведомлениям"))
        status = TextView(this).apply { textSize = 16f }
        root.addView(status)
        root.addView(Button(this).apply {
            text = "Открыть системное разрешение"
            setOnClickListener { withPin { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } }
        })

        root.addView(header("3. Проверка движка"))
        val input = EditText(this).apply {
            hint = "Например: никому не говори родителям, скинь фото"
            minLines = 3
            gravity = Gravity.TOP
        }
        val result = TextView(this).apply { textSize = 16f }
        root.addView(input)
        root.addView(Button(this).apply {
            text = "Проверить сообщение"
            setOnClickListener {
                val r = RiskEngine.analyze(input.text.toString())
                result.text = "Риск ${r.first}/100\n${r.second.joinToString(", ").ifBlank { "Триггеры не найдены" }}"
            }
        })
        root.addView(result)

        root.addView(header("4. Последние срабатывания"))
        journal = TextView(this).apply { textSize = 14f }
        root.addView(journal)
        root.addView(Button(this).apply {
            text = "Очистить журнал"
            setOnClickListener { withPin { prefs("alerts").edit().clear().apply(); refresh() } }
        })
        return scroll
    }

    private fun refresh() {
        status.text = if (listenerEnabled()) "✓ Защита сообщений активна" else "⚠ Доступ выключен — сообщения сейчас не проверяются"
        journal.text = prefs("alerts").getString("log", "Срабатываний пока нет.") ?: "Срабатываний пока нет."
    }

    private fun listenerEnabled(): Boolean =
        (Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: "").contains(packageName)

    private fun hasPin() = prefs("security").contains("pin")
    private fun hash(s: String) = MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    private fun withPin(action: () -> Unit) {
        if (!hasPin()) { action(); return }
        val i = EditText(this).apply {
            hint = "PIN родителя"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this).setTitle("Подтверждение родителя").setView(i)
            .setPositiveButton("Продолжить") { _, _ ->
                if (hash(i.text.toString()) == prefs("security").getString("pin", "")) action()
                else Toast.makeText(this, "Неверный PIN", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Отмена", null).show()
    }

    private fun askNewPin() {
        val i = EditText(this).apply {
            hint = "Минимум 4 цифры"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }
        AlertDialog.Builder(this).setTitle("Новый PIN").setView(i)
            .setPositiveButton("Сохранить") { _, _ ->
                val p = i.text.toString()
                if (p.length >= 4) {
                    prefs("security").edit().putString("pin", hash(p)).apply()
                    recreate()
                } else Toast.makeText(this, "PIN слишком короткий", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Отмена", null).show()
    }

    private fun header(t: String) = TextView(this).apply {
        text = t
        textSize = 19f
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(22), 0, dp(8))
    }

    private fun prefs(name: String) = getSharedPreferences("ryadom_$name", MODE_PRIVATE)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
