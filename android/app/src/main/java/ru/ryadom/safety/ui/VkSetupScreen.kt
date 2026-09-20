package ru.ryadom.safety.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ru.ryadom.safety.vk.VkCore
import ru.ryadom.safety.vk.VkState

@Composable
fun VkSetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val state by VkCore.state.collectAsState()
    var token by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ArrowBack, contentDescription = "Назад")
                }
                Spacer(Modifier.width(4.dp))
                Column {
                    Text(
                        "VK",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Прямое подключение сообщений",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(listOf(Navy, DeepTeal)))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.14f)) {
                        Icon(
                            Icons.Rounded.Send,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(13.dp).size(30.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            if (state is VkState.Ready) "VK подключён напрямую" else "Прямой канал VK",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Сообщения берутся из VK API, а не из уведомлений",
                            color = Color.White.copy(alpha = 0.78f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (val current = state) {
                        VkState.Ready -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = Success)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Защита VK активна", fontWeight = FontWeight.Bold)
                                    Text(
                                        "Новые входящие сообщения проверяются через VK API.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = { VkCore.disconnect(context) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Отключить VK")
                            }
                        }

                        VkState.Connecting -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Подключаемся к VK…")
                            }
                        }

                        is VkState.Error -> {
                            Text("Ошибка VK", fontWeight = FontWeight.Bold)
                            Text(current.message, color = Danger)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Для тестовой сборки можно подключить пользовательский access token с доступом к сообщениям.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TokenInput(token, onToken = { token = it })
                            Button(
                                onClick = { VkCore.connect(context, token) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Подключить") }
                        }

                        VkState.Disconnected -> {
                            Text(
                                "Ядро прямого VK уже встроено. В конечной версии здесь будет обычная кнопка «Войти через VK ID».",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Для внутреннего теста пока используется access token разработчика — клиентам это поле показываться не будет.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TokenInput(token, onToken = { token = it })
                            Button(
                                onClick = { VkCore.connect(context, token) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Подключить VK для теста") }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Токен сохраняется зашифрованно в Android Keystore. В релизе его ввод заменит VK ID OAuth.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TokenInput(token: String, onToken: (String) -> Unit) {
    OutlinedTextField(
        value = token,
        onValueChange = onToken,
        label = { Text("VK access token") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions.Default
    )
}
