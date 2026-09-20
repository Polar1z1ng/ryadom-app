package ru.ryadom.safety.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.ryadom.safety.security.PinStore
import ru.ryadom.safety.storage.AlertEvent
import ru.ryadom.safety.storage.AlertStore
import ru.ryadom.safety.telegram.TelegramCore
import ru.ryadom.safety.telegram.TelegramState

private enum class Tab { HOME, EVENTS, SETTINGS }

@Composable
fun RyadomApp() {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val prefs = remember { context.getSharedPreferences("ryadom_ui", 0) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", systemDark)) }
    var tab by remember { mutableStateOf(Tab.HOME) }
    var telegramSetup by remember { mutableStateOf(false) }
    val telegramState by TelegramCore.state.collectAsState()
    var events by remember { mutableStateOf(AlertStore.read(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            events = AlertStore.read(context)
            delay(1200)
        }
    }

    RyadomTheme(darkTheme = dark) {
        if (telegramSetup) {
            TelegramSetupScreen(
                state = telegramState,
                onBack = { telegramSetup = false }
            )
        } else {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        NavItem(Tab.HOME, tab, "Главная", Icons.Rounded.Home) { tab = Tab.HOME }
                        NavItem(Tab.EVENTS, tab, "События", Icons.Rounded.NotificationsActive) { tab = Tab.EVENTS }
                        NavItem(Tab.SETTINGS, tab, "Настройки", Icons.Rounded.Settings) { tab = Tab.SETTINGS }
                    }
                }
            ) { padding ->
                AnimatedContent(
                    targetState = tab,
                    modifier = Modifier.padding(padding),
                    label = "tabs"
                ) { current ->
                    when (current) {
                        Tab.HOME -> HomeScreen(
                            telegramState = telegramState,
                            events = events,
                            onTelegram = { telegramSetup = true },
                            onNotificationSettings = {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            }
                        )
                        Tab.EVENTS -> EventsScreen(events)
                        Tab.SETTINGS -> SettingsScreen(
                            dark = dark,
                            onDarkChange = {
                                dark = it
                                prefs.edit().putBoolean("dark", it).apply()
                            },
                            onClearEvents = {
                                AlertStore.clear(context)
                                events = emptyList()
                            },
                            onTelegram = { telegramSetup = true }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NavItem(tab: Tab, selected: Tab, label: String, icon: ImageVector, onClick: () -> Unit) {
    NavigationBarItem(
        selected = tab == selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

@Composable
private fun HomeScreen(
    telegramState: TelegramState,
    events: List<AlertEvent>,
    onTelegram: () -> Unit,
    onNotificationSettings: () -> Unit
) {
    val context = LocalContext.current
    val listener = notificationListenerEnabled(context.packageName)
    val telegramReady = telegramState is TelegramState.Ready
    val protectionActive = telegramReady || listener

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 22.dp, 20.dp, 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            BrandHeader()
        }
        item {
            ProtectionHero(
                active = protectionActive,
                eventCount = events.size
            )
        }
        item {
            SectionTitle("Источники защиты", "Что сейчас контролирует «Рядом»")
        }
        item {
            SourceCard(
                icon = Icons.Rounded.Send,
                title = "Telegram",
                subtitle = if (telegramReady) "Подключён напрямую через TDLib" else telegramStatusText(telegramState),
                active = telegramReady,
                badge = if (telegramReady) "НАПРЯМУЮ" else "ПОДКЛЮЧИТЬ",
                onClick = onTelegram
            )
        }
        item {
            SourceCard(
                icon = Icons.Rounded.Sms,
                title = "VK и SMS",
                subtitle = if (listener) "Анализ уведомлений включён" else "Нужно разрешить доступ к уведомлениям",
                active = listener,
                badge = "УВЕДОМЛЕНИЯ",
                onClick = onNotificationSettings
            )
        }
        item {
            PrivacyCard()
        }
    }
}

@Composable
private fun BrandHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Navy),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Shield,
                contentDescription = null,
                tint = Aqua,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Рядом",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Семейная цифровая безопасность",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ProtectionHero(active: Boolean, eventCount: Int) {
    val colors = if (active) {
        listOf(Navy, DeepTeal, Color(0xFF247A79))
    } else {
        listOf(Color(0xFF4A3824), Color(0xFF7A5A28))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(colors))
            .padding(22.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.14f)
                ) {
                    Icon(
                        if (active) Icons.Rounded.Security else Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(12.dp).size(28.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        if (active) "Защита активна" else "Нужна настройка",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Text(
                        if (active) "Рядом следит за выбранными источниками" else "Подключите хотя бы один источник",
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricPill("Событий", eventCount.toString())
                MetricPill("Анализ", "локально")
            }
        }
    }
}

@Composable
private fun MetricPill(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.12f)
    ) {
        Column(Modifier.padding(horizontal = 15.dp, vertical = 10.dp)) {
            Text(value, color = Color.White, fontWeight = FontWeight.Bold)
            Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(Modifier.padding(top = 6.dp, bottom = 2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SourceCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    active: Boolean,
    badge: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp).size(26.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    StatusDot(active)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    badge,
                    color = if (active) Success else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusDot(active: Boolean) {
    Box(
        Modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(if (active) Success else Warning)
    )
}

@Composable
private fun PrivacyCard() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f))
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Приватность по умолчанию", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Обычные сообщения не показываются родителю. Локально анализируется текст, а в журнал попадают только риск-события.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun EventsScreen(events: List<AlertEvent>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 24.dp, 20.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("События", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Здесь только сообщения, которые превысили порог риска.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }

        if (events.isEmpty()) {
            item { EmptyEvents() }
        } else {
            items(events) { event -> EventCard(event) }
        }
    }
}

@Composable
private fun EmptyEvents() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    null,
                    tint = Success,
                    modifier = Modifier.padding(16.dp).size(32.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Всё спокойно", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
            Text(
                "Риск-событий пока нет",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EventCard(event: AlertEvent) {
    val severity = when {
        event.score >= 70 -> Danger
        event.score >= 40 -> Warning
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = severity.copy(alpha = 0.13f)) {
                    Text(
                        event.score.toString() + "/100",
                        color = severity,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(event.source + " · " + event.chat, fontWeight = FontWeight.Bold)
                    Text(event.time, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(event.categories, color = severity, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(5.dp))
            Text(event.text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SettingsScreen(
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    onClearEvents: () -> Unit,
    onTelegram: () -> Unit
) {
    val context = LocalContext.current
    var pinDialog by remember { mutableStateOf(false) }
    var clearDialog by remember { mutableStateOf(false) }

    if (pinDialog) {
        PinDialog(onDismiss = { pinDialog = false })
    }
    if (clearDialog) {
        AlertDialog(
            onDismissRequest = { clearDialog = false },
            title = { Text("Очистить события?") },
            text = { Text("Локальный журнал риск-событий будет удалён.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearEvents()
                    clearDialog = false
                }) { Text("Очистить") }
            },
            dismissButton = { TextButton(onClick = { clearDialog = false }) { Text("Отмена") } }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        Text("Настройки", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Защита и внешний вид", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))

        SettingsCard {
            SettingsRow(Icons.Rounded.Send, "Telegram", "Подключение и авторизация", onTelegram)
            HorizontalDivider()
            SettingsRow(
                Icons.Rounded.Key,
                "PIN родителя",
                if (PinStore.hasPin(context)) "Установлен" else "Не установлен"
            ) { pinDialog = true }
            HorizontalDivider()
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.DarkMode, null)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Тёмная тема", fontWeight = FontWeight.SemiBold)
                    Text("Спокойное оформление вечером", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = dark, onCheckedChange = onDarkChange)
            }
        }

        Spacer(Modifier.height(14.dp))
        SettingsCard {
            SettingsRow(Icons.Rounded.DeleteOutline, "Очистить события", "Удалить локальный журнал") {
                clearDialog = true
            }
        }

        Spacer(Modifier.height(14.dp))
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
        ) {
            Row(Modifier.padding(18.dp)) {
                Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(
                    "«Рядом» работает открыто: приложение и постоянная защита видимы на телефоне. Скрытого режима нет.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PinDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val already = PinStore.hasPin(context)
    var current by remember { mutableStateOf("") }
    var fresh by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (already) "Изменить PIN" else "Установить PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (already) {
                    OutlinedTextField(
                        value = current,
                        onValueChange = { current = it.filter(Char::isDigit).take(8) },
                        label = { Text("Текущий PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                }
                OutlinedTextField(
                    value = fresh,
                    onValueChange = { fresh = it.filter(Char::isDigit).take(8) },
                    label = { Text("Новый PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )
                if (error.isNotBlank()) Text(error, color = Danger)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    fresh.length < 4 -> error = "Минимум 4 цифры"
                    already && !PinStore.verify(context, current) -> error = "Неверный текущий PIN"
                    else -> {
                        PinStore.set(context, fresh)
                        onDismiss()
                    }
                }
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun TelegramSetupScreen(state: TelegramState, onBack: () -> Unit) {
    val context = LocalContext.current
    var apiId by remember { mutableStateOf("") }
    var apiHash by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var emailCode by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ChevronRight, null, modifier = Modifier.background(Color.Transparent))
                }
                Spacer(Modifier.width(4.dp))
                Column {
                    Text("Подключение Telegram", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Прямое подключение через TDLib", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(22.dp))
            StepHero(state)
            Spacer(Modifier.height(18.dp))

            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when (state) {
                        TelegramState.NeedCredentials -> {
                            Text("Шаг 1 · Ключи приложения", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Telegram требует API ID и API Hash для любого стороннего клиента. Они сохранятся только на этом телефоне в Android Keystore.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                            FilledTonalButton(
                                onClick = {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://my.telegram.org")))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.OpenInNew, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Открыть my.telegram.org")
                            }
                            Button(
                                onClick = {
                                    val id = apiId.toIntOrNull()
                                    if (id == null || apiHash.length < 10) {
                                        localError = "Проверь API ID и API Hash"
                                    } else {
                                        localError = ""
                                        TelegramCore.configure(context, id, apiHash)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Продолжить") }
                        }

                        TelegramState.NeedPhone -> {
                            AuthTitle("Шаг 2 · Номер телефона", "Введи номер Telegram в международном формате.")
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("+7 999 000-00-00") },
                                leadingIcon = { Icon(Icons.Rounded.PhoneAndroid, null) },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )
                            PrimaryAction("Получить код") { TelegramCore.submitPhone(phone) }
                        }

                        TelegramState.NeedCode -> {
                            AuthTitle("Шаг 3 · Код Telegram", "Telegram пришлёт код в приложение или другим разрешённым способом.")
                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it.filter(Char::isDigit).take(8) },
                                label = { Text("Код") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            PrimaryAction("Подтвердить код") { TelegramCore.submitCode(code) }
                        }

                        is TelegramState.NeedPassword -> {
                            AuthTitle(
                                "Двухэтапная защита",
                                if (state.hint.isBlank()) "Введи пароль Telegram 2FA." else "Подсказка: " + state.hint
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Пароль 2FA") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                            PrimaryAction("Войти") { TelegramCore.submitPassword(password) }
                        }

                        TelegramState.NeedEmail -> {
                            AuthTitle("Подтверждение e-mail", "Telegram запросил адрес электронной почты.")
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("E-mail") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )
                            PrimaryAction("Продолжить") { TelegramCore.submitEmail(email) }
                        }

                        TelegramState.NeedEmailCode -> {
                            AuthTitle("Код из e-mail", "Введи код подтверждения Telegram.")
                            OutlinedTextField(
                                value = emailCode,
                                onValueChange = { emailCode = it.trim() },
                                label = { Text("Код") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            PrimaryAction("Подтвердить") { TelegramCore.submitEmailCode(emailCode) }
                        }

                        is TelegramState.ConfirmOnOtherDevice -> {
                            AuthTitle("Подтверди вход", "Telegram просит подтвердить авторизацию на другом устройстве.")
                            Text(state.link, color = MaterialTheme.colorScheme.primary)
                        }

                        TelegramState.Ready -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Surface(shape = CircleShape, color = Success.copy(alpha = 0.14f)) {
                                    Icon(
                                        Icons.Rounded.CheckCircle,
                                        null,
                                        tint = Success,
                                        modifier = Modifier.padding(18.dp).size(38.dp)
                                    )
                                }
                                Spacer(Modifier.height(14.dp))
                                Text("Telegram подключён", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    "«Рядом» получает обновления Telegram напрямую, даже если уведомления выключены.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(16.dp))
                                PrimaryAction("Готово", onBack)
                            }
                        }

                        is TelegramState.Error -> {
                            AuthTitle("Не получилось подключиться", state.message)
                            OutlinedButton(
                                onClick = { TelegramCore.resumePrompt() },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Исправить и повторить") }
                        }

                        TelegramState.Starting -> {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 26.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(14.dp))
                                Text("Запускаем защищённую сессию Telegram…")
                            }
                        }
                    }

                    if (localError.isNotBlank()) {
                        Text(localError, color = Danger, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Код входа и пароль 2FA не сохраняются. API Hash хранится зашифрованно в Android Keystore.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StepHero(state: TelegramState) {
    val ready = state is TelegramState.Ready
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(Navy, DeepTeal)))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.14f)) {
                Icon(
                    Icons.Rounded.Send,
                    null,
                    tint = Color.White,
                    modifier = Modifier.padding(13.dp).size(30.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    if (ready) "Связь установлена" else "Прямой канал Telegram",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    if (ready) "TDLib получает сообщения напрямую" else "Не зависит от уведомлений Android",
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }
    }
}

@Composable
private fun AuthTitle(title: String, description: String) {
    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
    Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun PrimaryAction(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 14.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

private fun telegramStatusText(state: TelegramState): String = when (state) {
    TelegramState.NeedCredentials -> "Нужны API ID и API Hash"
    TelegramState.NeedPhone -> "Нужно ввести номер Telegram"
    TelegramState.NeedCode -> "Ожидается код входа"
    is TelegramState.NeedPassword -> "Нужен пароль 2FA"
    TelegramState.NeedEmail -> "Нужно подтверждение e-mail"
    TelegramState.NeedEmailCode -> "Ожидается код из e-mail"
    is TelegramState.ConfirmOnOtherDevice -> "Подтвердите вход на другом устройстве"
    is TelegramState.Error -> "Ошибка подключения"
    TelegramState.Starting -> "Запуск защищённой сессии…"
    TelegramState.Ready -> "Подключён"
}

@Composable
private fun notificationListenerEnabled(packageName: String): Boolean {
    val context = LocalContext.current
    val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: ""
    return enabled.contains(packageName)
}
