package ru.ryadom.safety.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ryadom.safety.telegram.TelegramCore
import ru.ryadom.safety.telegram.TelegramState

@Composable
fun TelegramSetupScreen(state: TelegramState, onBack: () -> Unit) {
    val context = LocalContext.current
    var apiId by remember { mutableStateOf("") }
    var apiHash by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var emailCode by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf("") }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Назад") }
                Column {
                    Text("Telegram", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("Прямое подключение", color = SoftText)
                }
            }
            Spacer(Modifier.height(16.dp))
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (state) {
                        TelegramState.NeedCredentials -> {
                            Text("Тестовое подключение", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            Text(
                                "Для тестовой сборки нужны API ID и API Hash приложения Telegram. В версии для клиентов этих полей не будет.",
                                color = SoftText
                            )
                            OutlinedTextField(
                                value = apiId,
                                onValueChange = { apiId = it.filter(Char::isDigit) },
                                label = { Text("API ID") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = apiHash,
                                onValueChange = { apiHash = it.trim() },
                                label = { Text("API Hash") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedButton(
                                onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://my.telegram.org")))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.OpenInNew, null)
                                Spacer(Modifier.width(7.dp))
                                Text("Открыть my.telegram.org")
                            }
                            WarmButton("Продолжить") {
                                val id = apiId.toIntOrNull()
                                if (id == null || apiHash.length < 10) {
                                    localError = "Проверь API ID и API Hash"
                                } else {
                                    localError = ""
                                    TelegramCore.configure(context, id, apiHash)
                                }
                            }
                        }
                        TelegramState.NeedPhone -> {
                            SetupText("Номер телефона", "Введи номер Telegram в международном формате.")
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("+7 999 000-00-00") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )
                            WarmButton("Получить код") { TelegramCore.submitPhone(phone) }
                        }
                        TelegramState.NeedCode -> {
                            SetupText("Код Telegram", "Введи код, который прислал Telegram.")
                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it.filter(Char::isDigit).take(8) },
                                label = { Text("Код") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            WarmButton("Подтвердить") { TelegramCore.submitCode(code) }
                        }
                        is TelegramState.NeedPassword -> {
                            SetupText("Пароль 2FA", if (state.hint.isBlank()) "Введи пароль двухэтапной защиты." else "Подсказка: " + state.hint)
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Пароль") },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation()
                            )
                            WarmButton("Войти") { TelegramCore.submitPassword(password) }
                        }
                        TelegramState.NeedEmail -> {
                            SetupText("E-mail", "Telegram запросил адрес электронной почты.")
                            OutlinedTextField(email, { email = it }, label = { Text("E-mail") }, modifier = Modifier.fillMaxWidth())
                            WarmButton("Продолжить") { TelegramCore.submitEmail(email) }
                        }
                        TelegramState.NeedEmailCode -> {
                            SetupText("Код из e-mail", "Введи код подтверждения.")
                            OutlinedTextField(emailCode, { emailCode = it }, label = { Text("Код") }, modifier = Modifier.fillMaxWidth())
                            WarmButton("Подтвердить") { TelegramCore.submitEmailCode(emailCode) }
                        }
                        is TelegramState.ConfirmOnOtherDevice -> {
                            SetupText("Подтверди вход", "Telegram просит подтвердить авторизацию на другом устройстве.")
                            Text(state.link, color = Bronze)
                        }
                        TelegramState.Ready -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = Success)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Telegram подключён", fontWeight = FontWeight.Bold)
                                    Text("Сообщения получаются напрямую.", color = SoftText)
                                }
                            }
                            WarmButton("Готово", onBack)
                        }
                        is TelegramState.Error -> {
                            SetupText("Не получилось подключиться", state.message)
                            OutlinedButton(
                                onClick = { TelegramCore.resumePrompt() },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Повторить") }
                        }
                        TelegramState.Starting -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Запускаем Telegram…")
                            }
                        }
                    }
                    if (localError.isNotBlank()) Text(localError, color = Danger)
                }
            }
        }
    }
}

@Composable
private fun SetupText(title: String, description: String) {
    Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
    Text(description, color = SoftText)
}

@Composable
private fun WarmButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Bronze)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}
