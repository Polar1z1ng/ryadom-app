package ru.ryadom.safety.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
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
import kotlinx.coroutines.launch
import ru.ryadom.safety.R
import ru.ryadom.safety.contacts.CloseContactStore
import ru.ryadom.safety.family.FamilyMember
import ru.ryadom.safety.family.FamilyStore
import ru.ryadom.safety.profile.ProfileStore
import ru.ryadom.safety.profile.UserProfile
import ru.ryadom.safety.security.PinStore
import ru.ryadom.safety.sms.SmsDirect
import ru.ryadom.safety.storage.AlertEvent
import ru.ryadom.safety.storage.AlertStore
import ru.ryadom.safety.telegram.TelegramCore
import ru.ryadom.safety.telegram.TelegramState
import ru.ryadom.safety.vk.VkCore
import ru.ryadom.safety.vk.VkState

private enum class Tab { HOME, EVENTS, STATS, SETTINGS }
private enum class OverlayScreen { FAMILY, PROFILE, NOTIFICATIONS, HELP, ABOUT }
private enum class EventFilter { ALL, ALERT, ATTENTION, INFO }

@Composable
fun RyadomApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ryadom_ui", 0) }
    var introDone by remember { mutableStateOf(false) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", false)) }
    var tab by remember { mutableStateOf(Tab.HOME) }
    var telegramSetup by remember { mutableStateOf(false) }
    var vkSetup by remember { mutableStateOf(false) }
    var smsSetup by remember { mutableStateOf(false) }
    var selectedEvent by remember { mutableStateOf<AlertEvent?>(null) }
    var overlay by remember { mutableStateOf<OverlayScreen?>(null) }
    var familyMembers by remember { mutableStateOf(FamilyStore.read(context)) }

    val telegramState by TelegramCore.state.collectAsState()
    val vkState by VkCore.state.collectAsState()
    var events by remember { mutableStateOf(AlertStore.read(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            events = AlertStore.read(context)
            familyMembers = FamilyStore.read(context)
            delay(1000)
        }
    }

    RyadomTheme(darkTheme = dark) {
        when {
            !introDone -> IntroScreen {
                introDone = true
            }
            telegramSetup -> TelegramSetupScreen(telegramState) { telegramSetup = false }
            vkSetup -> VkSetupScreen { vkSetup = false }
            smsSetup -> SmsSetupScreen { smsSetup = false }
            overlay == OverlayScreen.FAMILY -> FamilyScreen(onBack = { overlay = null })
            overlay == OverlayScreen.PROFILE -> ProfileScreen(onBack = { overlay = null })
            overlay == OverlayScreen.NOTIFICATIONS -> NotificationSettingsScreen(onBack = { overlay = null })
            overlay == OverlayScreen.HELP -> HelpScreen(onBack = { overlay = null })
            overlay == OverlayScreen.ABOUT -> AboutScreen(onBack = { overlay = null })
            else -> MainScaffold(
                tab = tab,
                onTab = { tab = it },
                telegramState = telegramState,
                vkState = vkState,
                smsReady = SmsDirect.hasPermissions(context),
                events = events,
                familyCount = familyMembers.size,
                dark = dark,
                onDarkChange = {
                    dark = it
                    prefs.edit().putBoolean("dark", it).apply()
                },
                onTelegram = { telegramSetup = true },
                onVk = { vkSetup = true },
                onSms = { smsSetup = true },
                onOpenFamily = { overlay = OverlayScreen.FAMILY },
                onOpenProfile = { overlay = OverlayScreen.PROFILE },
                onOpenNotifications = { overlay = OverlayScreen.NOTIFICATIONS },
                onOpenHelp = { overlay = OverlayScreen.HELP },
                onOpenAbout = { overlay = OverlayScreen.ABOUT },
                onEvent = {
                    AlertStore.markAcknowledged(context, it.id)
                    events = AlertStore.read(context)
                    selectedEvent = it.copy(acknowledged = true)
                },
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
    familyCount: Int,
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    onTelegram: () -> Unit,
    onVk: () -> Unit,
    onSms: () -> Unit,
    onOpenFamily: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenAbout: () -> Unit,
    onEvent: (AlertEvent) -> Unit,
    onClearEvents: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun chooseTab(next: Tab) {
        onTab(next)
        scope.launch { drawerState.close() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = WarmWhite,
                modifier = Modifier.width(292.dp)
            ) {
                Spacer(Modifier.height(30.dp))
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_launcher),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Рядом", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text("Главное — быть рядом", color = SoftText, fontSize = 11.sp)
                    }
                }
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("Главная") },
                    selected = tab == Tab.HOME,
                    icon = { Icon(Icons.Rounded.Home, null) },
                    onClick = { chooseTab(Tab.HOME) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Семья") },
                    selected = false,
                    icon = { Icon(Icons.Rounded.Groups, null) },
                    badge = { if (familyCount > 0) Text(familyCount.toString()) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenFamily()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("События") },
                    selected = tab == Tab.EVENTS,
                    icon = { Icon(Icons.Rounded.EventNote, null) },
                    onClick = { chooseTab(Tab.EVENTS) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Статистика") },
                    selected = tab == Tab.STATS,
                    icon = { Icon(Icons.Rounded.BarChart, null) },
                    onClick = { chooseTab(Tab.STATS) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Настройки") },
                    selected = tab == Tab.SETTINGS,
                    icon = { Icon(Icons.Rounded.Settings, null) },
                    onClick = { chooseTab(Tab.SETTINGS) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                NavigationDrawerItem(
                    label = { Text("Профиль") },
                    selected = false,
                    icon = { Icon(Icons.Rounded.Person, null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenProfile()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
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
                        telegramState, vkState, smsReady, events, familyCount,
                        onTelegram, onVk, onSms, onEvent,
                        onAllEvents = { onTab(Tab.EVENTS) },
                        onFamily = onOpenFamily,
                        onMenu = { scope.launch { drawerState.open() } },
                        onProfile = onOpenProfile
                    )
                    Tab.EVENTS -> EventsScreen(events, onEvent)
                    Tab.STATS -> StatisticsScreen(events)
                    Tab.SETTINGS -> SettingsScreen(
                        dark, onDarkChange, onTelegram, onVk, onSms, onClearEvents,
                        onProfile = onOpenProfile,
                        onNotifications = onOpenNotifications,
                        onHelp = onOpenHelp,
                        onAbout = onOpenAbout
                    )
                }
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
        delay(3000)
        onStart()
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onStart),
        color = Cream
    ) {
        Image(
            painter = painterResource(R.drawable.ryadom_splash_exact),
            contentDescription = "Рядом — главное быть рядом",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun HomeScreen(
    telegramState: TelegramState,
    vkState: VkState,
    smsReady: Boolean,
    events: List<AlertEvent>,
    familyCount: Int,
    onTelegram: () -> Unit,
    onVk: () -> Unit,
    onSms: () -> Unit,
    onEvent: (AlertEvent) -> Unit,
    onAllEvents: () -> Unit,
    onFamily: () -> Unit,
    onMenu: () -> Unit,
    onProfile: () -> Unit
) {
    val telegramReady = telegramState is TelegramState.Ready
    val vkReady = vkState is VkState.Ready
    val activeCount = listOf(telegramReady, vkReady, smsReady).count { it }
    val telegramAlert = events.any { !it.acknowledged && it.source.equals("Telegram", true) }
    val vkAlert = events.any {
        !it.acknowledged && (it.source.equals("VK", true) || it.source.contains("ВКонтакте", true))
    }
    val smsAlert = events.any { !it.acknowledged && it.source.equals("SMS", true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 16.dp, 18.dp, 26.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Header(onMenu = onMenu, onProfile = onProfile) }
        item { ProtectionCard(activeCount) }
        item { FamilyShortcutCard(familyCount = familyCount, onClick = onFamily) }
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
                alert = telegramAlert,
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
                alert = vkAlert,
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
                alert = smsAlert,
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
private fun Header(onMenu: () -> Unit, onProfile: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMenu) {
            Icon(Icons.Rounded.Menu, "Меню", tint = DeepBrown)
        }
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Text("Рядом", fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("Главное — быть рядом", color = SoftText, fontSize = 12.sp)
        }
        IconButton(onClick = onProfile) {
            Icon(Icons.Rounded.Person, "Профиль", tint = Bronze)
        }
    }
}

@Composable
private fun FamilyShortcutCard(familyCount: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Sand.copy(alpha = 0.38f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = RoundedCornerShape(14.dp), color = Bronze) {
                Icon(
                    Icons.Rounded.Groups,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(10.dp).size(26.dp)
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("Семья", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(
                    if (familyCount == 0) "Добавьте близких" else "Близких добавлено: $familyCount",
                    color = SoftText,
                    fontSize = 12.sp
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Bronze)
        }
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
    alert: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        border = if (alert) BorderStroke(2.dp, Danger) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (alert) DangerSoft.copy(alpha = 0.48f) else WarmWhite
        )
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (alert) Danger else brandColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        brand,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (brand == "SMS") 10.sp else 16.sp
                    )
                }
                if (alert) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).size(21.dp),
                        shape = CircleShape,
                        color = Danger,
                        border = BorderStroke(2.dp, WarmWhite)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("!", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = if (alert) Danger else MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(7.dp).clip(CircleShape)
                            .background(if (alert) Danger else if (active) Success else Warning)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        if (alert) "Новое риск-событие" else subtitle,
                        color = if (alert) Danger else if (active) Success else SoftText,
                        fontSize = 12.sp,
                        fontWeight = if (alert) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
            Icon(
                if (alert) Icons.Rounded.PriorityHigh else Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = if (alert) Danger else SoftText
            )
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
    onClearEvents: () -> Unit,
    onProfile: () -> Unit,
    onNotifications: () -> Unit,
    onHelp: () -> Unit,
    onAbout: () -> Unit
) {
    val context = LocalContext.current
    var pinDialog by remember { mutableStateOf(false) }
    var clearDialog by remember { mutableStateOf(false) }
    var closeContactDialog by remember { mutableStateOf(false) }
    var closeNumber by remember { mutableStateOf(CloseContactStore.number(context)) }

    if (pinDialog) {
        PinDialog { pinDialog = false }
    }
    if (closeContactDialog) {
        CloseContactDialog(
            initial = closeNumber,
            onSaved = {
                closeNumber = CloseContactStore.number(context)
                closeContactDialog = false
            },
            onDismiss = { closeContactDialog = false }
        )
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
                SettingsLine(Icons.Rounded.Person, "Профиль", "Личные данные") { onProfile() }
                HorizontalDivider()
                SettingsLine(
                    Icons.Rounded.Phone,
                    "Близкий для экстренного звонка",
                    if (closeNumber.isBlank()) "Указать номер" else closeNumber
                ) { closeContactDialog = true }
                HorizontalDivider()
                SettingsLine(
                    Icons.Rounded.Lock,
                    "Безопасность (PIN-код)",
                    if (PinStore.hasPin(context)) "PIN установлен" else "Установить PIN"
                ) { pinDialog = true }
                HorizontalDivider()
                SettingsLine(Icons.Rounded.Notifications, "Уведомления", "Звук, баннеры и разрешения") { onNotifications() }
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
                SettingsLine(Icons.Rounded.HelpOutline, "Помощь", "Как работает «Рядом»") { onHelp() }
                HorizontalDivider()
                SettingsLine(Icons.Rounded.Info, "О приложении", "Рядом · семейная безопасность") { onAbout() }
                HorizontalDivider()
                SettingsLine(Icons.Rounded.DeleteOutline, "Очистить события", "Удалить локальный журнал") {
                    clearDialog = true
                }
            }
        }
    }
}

@Composable
private fun FamilyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var members by remember { mutableStateOf(FamilyStore.read(context)) }
    var addDialog by remember { mutableStateOf(false) }
    var deleteMember by remember { mutableStateOf<FamilyMember?>(null) }

    if (addDialog) {
        AddFamilyMemberDialog(
            onDismiss = { addDialog = false },
            onAdd = { name, role, phone ->
                FamilyStore.add(context, name, role, phone)
                members = FamilyStore.read(context)
                addDialog = false
            }
        )
    }

    deleteMember?.let { member ->
        AlertDialog(
            onDismissRequest = { deleteMember = null },
            title = { Text("Убрать из семьи?") },
            text = { Text(member.name) },
            confirmButton = {
                TextButton(onClick = {
                    FamilyStore.remove(context, member.id)
                    members = FamilyStore.read(context)
                    deleteMember = null
                }) { Text("Убрать", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { deleteMember = null }) { Text("Отмена") } }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SimpleTopBar("Семья", onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { addDialog = true },
                containerColor = Bronze,
                contentColor = Color.White,
                icon = { Icon(Icons.Rounded.PersonAdd, null) },
                text = { Text("Добавить") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(18.dp, 12.dp, 18.dp, 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Близкие",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Добавляйте детей, родителей и пожилых близких. Привязку отдельных устройств сделаем через семейное подключение.",
                    color = SoftText,
                    fontSize = 13.sp
                )
            }
            if (members.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = WarmWhite)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Rounded.Groups, null, tint = Bronze, modifier = Modifier.size(42.dp))
                            Spacer(Modifier.height(10.dp))
                            Text("Пока никого нет", fontWeight = FontWeight.Bold)
                            Text("Нажмите «Добавить», чтобы создать семейный список.", color = SoftText, fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(members, key = { it.id }) { member ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = WarmWhite)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(15.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = CircleShape, color = Sand.copy(alpha = 0.7f)) {
                                Icon(
                                    when {
                                        member.role.contains("Пожил", true) -> Icons.Rounded.Elderly
                                        member.role.contains("Реб", true) -> Icons.Rounded.ChildCare
                                        else -> Icons.Rounded.Person
                                    },
                                    null,
                                    tint = Bronze,
                                    modifier = Modifier.padding(10.dp).size(25.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(member.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(member.role, color = SoftText, fontSize = 12.sp)
                                if (member.phone.isNotBlank()) {
                                    Text(member.phone, color = SoftText, fontSize = 11.sp)
                                }
                            }
                            IconButton(onClick = { deleteMember = member }) {
                                Icon(Icons.Rounded.DeleteOutline, "Убрать", tint = Danger)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddFamilyMemberDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Ребёнок") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить близкого") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(50) },
                    label = { Text("Имя") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Кто это?", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Ребёнок", "Родитель", "Пожилой").forEach { option ->
                        FilterChip(
                            selected = role == option,
                            onClick = { role = option },
                            label = { Text(option, fontSize = 11.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.take(24) },
                    label = { Text("Телефон (необязательно)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error.isNotBlank()) Text(error, color = Danger)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.trim().length < 2) error = "Укажите имя"
                else onAdd(name, role, phone)
            }) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
private fun ProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val initial = remember { ProfileStore.read(context) }
    var name by remember { mutableStateOf(initial.fullName) }
    var birthDate by remember { mutableStateOf(initial.birthDate) }
    var saved by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SimpleTopBar("Профиль", onBack) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(shape = CircleShape, color = Sand.copy(alpha = 0.7f)) {
                Icon(
                    Icons.Rounded.Person,
                    null,
                    tint = Bronze,
                    modifier = Modifier.padding(18.dp).size(36.dp)
                )
            }
            Text("Личные данные", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(80); saved = false },
                label = { Text("ФИО") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = birthDate,
                onValueChange = { birthDate = it.take(10); saved = false },
                label = { Text("Дата рождения") },
                placeholder = { Text("ДД.ММ.ГГГГ") },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    ProfileStore.save(context, UserProfile(name, birthDate))
                    saved = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Bronze)
            ) { Text("Сохранить") }
            if (saved) Text("Сохранено", color = Success)
        }
    }
}

@Composable
private fun NotificationSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SimpleTopBar("Уведомления", onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = WarmWhite)) {
                    Column(Modifier.padding(18.dp)) {
                        Icon(Icons.Rounded.NotificationsActive, null, tint = Danger, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("Тревожные уведомления", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Для риска «Рядом» использует звук, вибрацию и повторные предупреждения при высоком уровне.",
                            color = SoftText,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Bronze)
                ) {
                    Icon(Icons.Rounded.Settings, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Открыть настройки Android")
                }
            }
            item {
                Text(
                    "Там можно включить всплывающие баннеры, звук на заблокированном экране и разрешить уведомления высокой важности.",
                    color = SoftText,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun HelpScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SimpleTopBar("Помощь", onBack) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { HelpCard("Как работает «Рядом»", "Приложение анализирует подключённые источники и сохраняет только риск-события.") }
            item { HelpCard("Что означают уровни риска", "Чем выше оценка, тем срочнее событие. Высокий риск сопровождается повторными предупреждениями.") }
            item { HelpCard("Экстренный звонок", "Укажите номер близкого в настройках — тогда в тревоге появится кнопка быстрого звонка.") }
            item { HelpCard("Семья", "В разделе «Семья» можно вести список близких. Следующим этапом будет привязка отдельных устройств через семейное подключение.") }
        }
    }
}

@Composable
private fun HelpCard(title: String, body: String) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = WarmWhite)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(body, color = SoftText, fontSize = 13.sp)
        }
    }
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { SimpleTopBar("О приложении", onBack) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher),
                contentDescription = null,
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(24.dp))
            )
            Spacer(Modifier.height(14.dp))
            Text("Рядом", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Главное — быть рядом", color = Cocoa)
            Spacer(Modifier.height(18.dp))
            Text(
                "Семейная безопасность для детей, родителей и пожилых близких.",
                color = SoftText
            )
            Spacer(Modifier.height(8.dp))
            Text("Версия 0.6.0", color = SoftText, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, "Назад")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
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
private fun CloseContactDialog(
    initial: String,
    onSaved: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var number by remember { mutableStateOf(initial) }
    var error by remember { mutableStateOf("") }

    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Близкий для экстренного звонка") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Этот номер будет доступен одной кнопкой при тревожном событии.",
                    color = SoftText
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it.take(24) },
                    label = { Text("Номер телефона") },
                    placeholder = { Text("+7 900 000-00-00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error.isNotBlank()) Text(error, color = Danger)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val digits = number.filter(Char::isDigit)
                if (digits.length < 7) {
                    error = "Проверь номер телефона"
                } else {
                    CloseContactStore.save(context, number)
                    if (!CloseContactStore.hasCallPermission(context)) {
                        callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                    }
                    onSaved()
                }
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
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
    val context = LocalContext.current
    val color = severityColor(event.score)
    val closeNumber = CloseContactStore.number(context)
    var callAfterPermission by remember { mutableStateOf(false) }
    val callPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && callAfterPermission) {
            CloseContactStore.call(context)
        }
        callAfterPermission = false
    }
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
                if (closeNumber.isNotBlank()) {
                    Button(
                        onClick = {
                            if (CloseContactStore.hasCallPermission(context)) {
                                CloseContactStore.call(context)
                            } else {
                                callAfterPermission = true
                                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Danger),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.Phone, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Позвонить близкому")
                    }
                } else if (event.score >= 70) {
                    Text(
                        "Добавь номер близкого в Настройки → Близкий для экстренного звонка.",
                        color = SoftText,
                        fontSize = 12.sp
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
