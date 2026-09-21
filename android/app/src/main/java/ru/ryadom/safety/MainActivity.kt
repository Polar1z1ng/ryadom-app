package ru.ryadom.safety

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ru.ryadom.safety.telegram.TelegramCore
import ru.ryadom.safety.ui.RyadomApp
import ru.ryadom.safety.vk.VkCore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
        }

        setContent {
            RyadomApp()
        }

        // UI must always open even if an external integration fails to initialize.
        window.decorView.post {
            runCatching { TelegramCore.ensureStarted(applicationContext) }
            runCatching { VkCore.ensureStarted(applicationContext) }
        }
    }
}
