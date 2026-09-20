package ru.ryadom.safety.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.ryadom.safety.R
import ru.ryadom.safety.security.PinStore
import ru.ryadom.safety.sms.SmsDirect
import ru.ryadom.safety.storage.AlertEvent
import ru.ryadom.safety.storage.AlertStore
import ru.ryadom.safety.telegram.TelegramCore
import ru.ryadom.safety.telegram.TelegramState
import ru.ryadom.safety.vk.VkCore
import ru.ryadom.safety.vk.VkState

private enum class Tab { HOME, EVENTS, STATS, SETTINGS }
private enum class EventFilter { ALL, ALERT, ATTENTION, INFO }

@Composable
fun RyadomApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ryadom_ui", 0) }
    var introDone by remember { mutableStateOf(prefs.getBoolean("intro_done", false)) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", false)) }
    var tab by remember { mutableStateOf(Tab.HOME) }
    var telegramSetup by remember { mutableStateOf(false) }
    var vkSetup by remember { mutableStateOf(false) }
    var smsSetup by remember { mutableStateOf(false) }
    var selectedEvent by remember { mutableStateOf<AlertEvent?>(null) }

    val telegramState by TelegramCore.state.collectAsState()
    val vkState by VkCore.state.collectAsState()
    var events by remember { mutableStateOf(AlertStore.read(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            events = AlertStore.read(context)
            delay(1000)
        }
    }

    RyadomTheme(darkTheme = dark) {
        when {
            !introDone -> IntroScreen {
                prefs.edit().putBoolean("intro_done", true).apply()
                introDone = true
            }
            telegramSetup -> TelegramSetupScreen(telegramState) { telegramSetup = false }
            vkSetup -> VkSetupScreen { vkSetup = false }
            smsSetup -> SmsSetupScreen { smsSetup = false }
            else -> MainScaffold(
                tab = tab,
                onTab = { tab = it },
                telegramState = telegramState,
                vkState = vkState,
                smsReady = SmsDirect.hasPermissions(context),
                events = events,
                dark = dark,
                onDarkChange = {
                    dark = it
                    prefs.edit().putBoolean("dark", it).apply()
                },
                onTelegram = { telegramSetup = true },
                onVk = { vkSetup = true },
                onSms = { smsSetup = true },
                onEvent = { selectedEvent = it },
                onClearEvents = {
                    AlertStore.clear(context)
                    events = emptyList()
                }
            )
        }

        selectedEvent?.let {
            EventDetailDialog(event = it, onDismiss = { selectedEvent = null })
        }
    }
}

@Composable
private fun MainScaffold(
    tab: Tab,
    onTab: (Tab) -> Unit,
    telegramState: TelegramState,
    vkState: VkState,
    smsReady: Boolean,
    events: List<AlertEvent>,
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    onTelegram: () -> Unit,
    onVk: () -> Unit,
    onSms: () -> Unit,
    onEvent: (AlertEvent) -> Unit,
    onClearEvents: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavItem(Tab.HOME, tab, "Главная", Icons.Rounded.Home, onTab)
                NavItem(Tab.EVENTS, tab, "События", Icons.Rounded.EventNote, onTab)
                NavItem(Tab.STATS, tab, "Статистика", Icons.Rounded.BarChart, onTab)
                NavItem(Tab.SETTINGS, tab, "Настройки", Icons.Rounded.Settings, onTab)
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
                    telegramState, vkState, smsReady, events,
                    onTelegram, onVk, onSms, onEvent,
                    onAllEvents = { onTab(Tab.EVENTS) }
                )
                Tab.EVENTS -> EventsScreen(events, onEvent)
                Tab.STATS -> StatisticsScreen(events)
                Tab.SETTINGS -> SettingsScreen(
                    dark, onDarkChange, onTelegram, onVk, onSms, onClearEvents
                )
            }
        }
    }
}

@Composable
private fun RowScope.NavItem(
    item: Tab,
    selected: Tab,
    label: String,
    icon: ImageVector,
    onTab: (Tab) -> Unit
) {
    NavigationBarItem(
        selected = item == selected,
        onClick = { onTab(item) },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label, fontSize = 11.sp) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Bronze,
            selectedTextColor = Bronze,
            indicatorColor = Sand.copy(alpha = 0.65f)
        )
    )
}

@Composable
private fun IntroScreen(onStart: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2600)
        onStart()
    }

    Surface(
        modifier = Modifier.fillMaxSize().clickable(onClick = onStart),
        color = Cream
    ) {
        Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 56.dp, start = 28.dp, end = 28.dp, bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher),
                    contentDescription = "Рядом",
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(24.dp))
                )
                Spacer(Modifier.height(11.dp))
                Text(
                    "Рядом",
                    color = DeepBrown,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Главное — быть рядом",
                    color = Cocoa,
                    fontSize = 17.sp
                )
            }

            Image(
                painter = painterResource(R.drawable.mom_boy),
                contentDescription = "Мама обнимает мальчика",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun HomeScreen(
    telegramState: TelegramState,
    vkState: VkState,
    smsReady: Boolean,
    events: List<AlertEvent>,
    onTelegram: () -> Unit,
    onVk: () -> Unit,
    onSms: () -> Unit,
    onEvent: (AlertEvent) -> Unit,
    onAllEvents: () -> Unit
) {
    val telegramReady = telegramState is TelegramState.Ready
    val vkReady = vkState is VkState.Ready
    val activeCount = listOf(telegramReady, vkReady, smsReady).count { it }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 16.dp, 18.dp, 26.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Header() }
        item { ProtectionCard(activeCount) }
        item {
            Text("Подключенные сервисы", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
        item {
            ServiceCard(
                title = "Telegram",
                subtitle = if (telegramReady) "Подключено" else telegramStateLabel(telegramState),
                active = telegramReady,
                brand = "TG",
                brandColor = Color(0xFF229ED9),
                onClick = onTelegram
            )
        }
        item {
            ServiceCard(
                title = "ВКонтакте",
                subtitle = when (vkState) {
                    VkState.Ready -> "Подключено"
                    VkState.Connecting -> "Подключение…"
                    is VkState.Error -> "Нужно проверить"
                    VkState.Disconnected -> "Подключить"
                },
                active = vkReady,
                brand = "VK",
                brandColor = Color(0xFF2787F5),
                onClick = onVk
            )
        }
        item {
            ServiceCard(
                title = "SMS",
                subtitle = if (smsReady) "Подключено" else "Разрешить доступ",
                active = smsReady,
                brand = "SMS",
                brandColor = Color(0xFF32B85A),
                onClick = onSms
            )
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Последние события", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                TextButton(onClick = onAllEvents) { Text("Все", color = Bronze) }
            }
        }

        if (events.isEmpty()) {
            item { QuietCard() }
        } else {
            items(events.take(4)) { event ->
                EventRow(event) { onEvent(event) }
            }
        }
    }
}

@Composable
private fun Header() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Menu, null, tint = DeepBrown)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Рядом", fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("Главное — быть рядом", color = SoftText, fontSize = 12.sp)
        }
        Icon(Icons.Rounded.Person, null, tint = Bronze)
    }
}

@Composable
private fun ProtectionCard(activeCount: Int) {
    val active = activeCount > 0
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) SuccessSoft else Color(0xFFFFF0DA)
        )
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (active) Success else Warning
            ) {
                Icon(
                    Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(10.dp).size(26.dp)
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (active) "Защита активна" else "Нужна настройка",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    if (active) "Мы следим за важными сообщениями" else "Подключите Telegram, VK или SMS",
                    color = SoftText,
                    fontSize = 12.sp
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = SoftText)
        }
    }
}

@Composable
private fun ServiceCard(
    title: String,
    subtitle: String,
    active: Boolean,
    brand: String,
    brandColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WarmWhite)
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(brandColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    brand,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (brand == "SMS") 10.sp else 16.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(if (active) Success else Warning))
                    Spacer(Modifier.width(5.dp))
                    Text(subtitle, color = if (active) Success else SoftText, fontSize = 12.sp)
                }
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = SoftText)
        }
    }
}

@Composable
private fun QuietCard() {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WarmWhite)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Favorite, null, tint = Success)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Всё спокойно", fontWeight = FontWeight.Bold)
                Text("Тревожных сигналов пока нет", color = SoftText, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun EventRow(event: AlertEvent, onClick: () -> Unit) {
    val color = severityColor(event.score)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WarmWhite)
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = color.copy(alpha = 0.13f)) {
                Icon(
                    if (event.score >= 70) Icons.Rounded.PriorityHigh else Icons.Rounded.Info,
                    null,
                    tint = color,
                    modifier = Modifier.padding(8.dp).size(19.dp)
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    eventTitle(event),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    event.text,
                    color = SoftText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(event.source, color = SoftText, fontSize = 11.sp)
            }
            Text(event.time, color = SoftText, fontSize = 11.sp)
        }
    }
}

@Composable
private fun EventsScreen(events: List<AlertEvent>, onEvent: (AlertEvent) -> Unit) {
    var filter by remember { mutableStateOf(EventFilter.ALL) }
    val filtered = when (filter) {
        EventFilter.ALL -> events
        EventFilter.ALERT -> events.filter { it.score >= 70 }
        EventFilter.ATTENTION -> events.filter { it.score in 20..69 }
        EventFilter.INFO -> events.filter { it.score < 20 }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 26.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("События", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.Tune, null, tint = Bronze)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FilterPill("Все", filter == EventFilter.ALL) { filter = EventFilter.ALL }
                FilterPill("Тревожные", filter == EventFilter.ALERT) { filter = EventFilter.ALERT }
                FilterPill("Внимание", filter == EventFilter.ATTENTION) { filter = EventFilter.ATTENTION }
                FilterPill("Инфо", filter == EventFilter.INFO) { filter = EventFilter.INFO }
            }
        }
        if (filtered.isEmpty()) {
            item { QuietCard() }
        } else {
            items(filtered) { event -> EventRow(event) { onEvent(event) } }
        }
    }
}

@Composable
private fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Bronze else WarmWhite
    ) {
        Text(
            text,
            color = if (selected) Color.White else DeepBrown,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun StatisticsScreen(events: List<AlertEvent>) {
    val high = events.count { it.score >= 70 }
    val attention = events.count { it.score in 20..69 }
    val fraud = events.count { it.categories.contains("мошенн", true) }
    val abuse = events.count {
        it.categories.contains("оскорб", true) ||
            it.categories.contains("трав", true) ||
            it.categories.contains("угроз", true)
    }
    val grooming = events.count {
        it.categories.contains("грум", true) ||
            it.categories.contains("сексу", true)
    }
    val max = maxOf(1, fraud, abuse, grooming)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 26.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Text("Статистика", fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = WarmWhite)) {
                Column(Modifier.padding(17.dp)) {
                    Text("Всего риск-событий", color = SoftText, fontSize = 12.sp)
                    Text(events.size.toString(), fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Metric("Тревожные", high, Danger, Modifier.weight(1f))
                        Metric("Внимание", attention, Warning, Modifier.weight(1f))
                        Metric("Инфо", events.size - high - attention, Info, Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = WarmWhite)) {
                Column(Modifier.padding(17.dp)) {
                    Text("Распределение по типам", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    StatBar("Мошенничество", fraud, max, Danger)
                    StatBar("Оскорбления / угрозы", abuse, max, Bronze)
                    StatBar("Груминг / сексуальные риски", grooming, max, Warning)
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = color.copy(alpha = 0.10f)) {
        Column(Modifier.padding(10.dp)) {
            Text(value.toString(), color = color, fontWeight = FontWeight.Bold, fontSize = 21.sp)
            Text(label, color = SoftText, fontSize = 10.sp)
        }
    }
}

@Composable
private fun StatBar(label: String, value: Int, max: Int, color: Color) {
    Column(Modifier.padding(vertical = 7.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp)
            Text(value.toString(), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Sand.copy(alpha = 0.55f))) {
            if (value > 0) {
                Box(
                    Modifier.fillMaxWidth((value.toFloat() / max.toFloat()).coerceIn(0.08f, 1f))
                        .fillMaxHeight().clip(CircleShape).background(color)
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    onTelegram: () -> Unit,
    onVk: () -> Unit,
    onSms: () -> Unit,
    onClearEvents: () -> Unit
) {
    val context = LocalContext.current
    var pinDialog by remember { mutableStateOf(false) }
    var clearDialog by remember { mutableStateOf(false) }

    if (pinDialog) {
        PinDialog { pinDialog = false }
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
            dismissButton = {
                TextButton(onClick = { clearDialog = false }) { Text("Отмена") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 18.dp, 18.dp, 26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Настройки", fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        item {
            Text("Аккаунт", color = SoftText, fontSize = 12.sp)
            SettingsCard {
                SettingsLine(Icons.Rounded.Person, "Профиль", "Семейная защита") {}
                HorizontalDivider()
                SettingsLine(
                    Icons.Rounded.Lock,
                    "Безопасность (PIN-код)",
                    if (PinStore.hasPin(context)) "PIN установлен" else "Установить PIN"
                ) { pinDialog = true }
                HorizontalDivider()
                SettingsLine(Icons.Rounded.Notifications, "Уведомления", "Системные оповещения") {}
            }
        }
        item {
            Text("Подключения", color = SoftText, fontSize = 12.sp)
            SettingsCard {
                SettingsLine(Icons.Rounded.Send, "Telegram", "Прямое подключение", onTelegram)
                HorizontalDivider()
                SettingsLine(Icons.Rounded.Forum, "ВКонтакте", "Прямое подключение", onVk)
                HorizontalDivider()
                SettingsLine(Icons.Rounded.Sms, "SMS", "Системный доступ Android", onSms)
            }
        }
        item {
            Text("Прочее", color = SoftText, fontSize = 12.sp)
            SettingsCard {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.DarkMode, null, tint = Bronze)
                    Spacer(Modifier.width(12.dp))
                    Text("Тёмная тема", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Switch(checked = dark, onCheckedChange = onDarkChange)
                }
                HorizontalDivider()
                SettingsLine(Icons.Rounded.HelpOutline, "Помощь", "Как работает «Рядом»") {}
                HorizontalDivider()
                SettingsLine(Icons.Rounded.Info, "О приложении", "Рядом · семейная безопасность") {}
                HorizontalDivider()
                SettingsLine(Icons.Rounded.DeleteOutline, "Очистить события", "Удалить локальный журнал") {
                    clearDialog = true
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = WarmWhite)) {
        Column(content = content)
    }
}

@Composable
private fun SettingsLine(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Bronze)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = SoftText, fontSize = 11.sp)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = SoftText)
    }
}

@Composable
private fun PinDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val exists = PinStore.hasPin(context)
    var current by remember { mutableStateOf("") }
    var fresh by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (exists) "Изменить PIN" else "Установить PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (exists) {
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
                    exists && !PinStore.verify(context, current) -> error = "Неверный текущий PIN"
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
private fun EventDetailDialog(event: AlertEvent, onDismiss: () -> Unit) {
    val color = severityColor(event.score)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(if (event.score >= 70) "Тревожный сигнал" else "Событие")
                Text(event.time, color = SoftText, fontSize = 12.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(event.source, color = SoftText, fontSize = 12.sp)
                Text(event.text)
                Text("Оценка риска", fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { event.score / 100f },
                        modifier = Modifier.weight(1f),
                        color = color,
                        trackColor = Sand.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(event.score.toString() + " / 100", fontWeight = FontWeight.Bold)
                }
                Surface(shape = RoundedCornerShape(14.dp), color = color.copy(alpha = 0.10f)) {
                    Text(
                        if (event.score >= 90)
                            "Высокий риск. Рекомендуем обратить внимание и связаться с близким."
                        else
                            event.categories,
                        modifier = Modifier.padding(12.dp),
                        color = DeepBrown
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Понятно") }
        }
    )
}

private fun eventTitle(event: AlertEvent): String {
    return when {
        event.score >= 70 -> "Тревожный сигнал"
        event.categories.contains("мошенн", true) -> "Потенциальный риск"
        event.categories.contains("оскорб", true) -> "Оскорбление"
        event.categories.contains("алког", true) -> "Упоминание алкоголя"
        else -> "Потенциальный риск"
    }
}

private fun severityColor(score: Int): Color = when {
    score >= 70 -> Danger
    score >= 20 -> Warning
    else -> Info
}

private fun telegramStateLabel(state: TelegramState): String = when (state) {
    TelegramState.Ready -> "Подключено"
    TelegramState.NeedCredentials -> "Нужна настройка"
    TelegramState.NeedPhone -> "Войти в Telegram"
    TelegramState.NeedCode -> "Ввести код"
    is TelegramState.NeedPassword -> "Пароль 2FA"
    TelegramState.NeedEmail -> "Подтвердить e-mail"
    TelegramState.NeedEmailCode -> "Код e-mail"
    is TelegramState.ConfirmOnOtherDevice -> "Подтвердить вход"
    TelegramState.Starting -> "Запуск…"
    is TelegramState.Error -> "Ошибка подключения"
}
