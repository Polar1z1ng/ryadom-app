package ru.ryadom.safety.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Cream = Color(0xFFF7EFE5)
val WarmWhite = Color(0xFFFFFAF4)
val Sand = Color(0xFFE8D5C0)
val Caramel = Color(0xFFB87942)
val Bronze = Color(0xFF8B5A32)
val DeepBrown = Color(0xFF4A2D19)
val Cocoa = Color(0xFF6D4A31)
val SoftText = Color(0xFF7A685B)

val Success = Color(0xFF6F8F61)
val SuccessSoft = Color(0xFFE7F0DE)
val Warning = Color(0xFFD7A23D)
val Danger = Color(0xFFD95A43)
val DangerSoft = Color(0xFFFFE8E1)
val Info = Color(0xFF8D8981)

// Имена сохранены для экранов подключения, но вся палитра теперь тёплая бежевая.
val Navy = DeepBrown
val DeepTeal = Bronze
val Teal = Caramel
val Aqua = Sand
val Mist = Cream
val Ink = DeepBrown
val SoftInk = SoftText

private val LightColors = lightColorScheme(
    primary = Bronze,
    onPrimary = Color.White,
    primaryContainer = Sand,
    onPrimaryContainer = DeepBrown,
    secondary = Caramel,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3DFC9),
    onSecondaryContainer = DeepBrown,
    tertiary = Success,
    onTertiary = Color.White,
    background = Cream,
    onBackground = DeepBrown,
    surface = WarmWhite,
    onSurface = DeepBrown,
    surfaceVariant = Color(0xFFF1E6DA),
    onSurfaceVariant = SoftText,
    outline = Color(0xFFD7C4B1),
    error = Danger,
    onError = Color.White,
    errorContainer = DangerSoft,
    onErrorContainer = Color(0xFF6C241B)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD9AC82),
    onPrimary = Color(0xFF3A210F),
    primaryContainer = Color(0xFF684425),
    onPrimaryContainer = Color(0xFFF6DDC5),
    secondary = Color(0xFFE5B88C),
    onSecondary = Color(0xFF3B2412),
    background = Color(0xFF211812),
    onBackground = Color(0xFFF3E6D8),
    surface = Color(0xFF2C211A),
    onSurface = Color(0xFFF3E6D8),
    surfaceVariant = Color(0xFF3A2C23),
    onSurfaceVariant = Color(0xFFD8C5B5),
    outline = Color(0xFF8E7968),
    error = Color(0xFFFFB4A7),
    onError = Color(0xFF680008)
)

@Composable
fun RyadomTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
