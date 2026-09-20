package ru.ryadom.safety.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Тёплая фирменная палитра «Рядом», подобранная под выбранную иконку.
val Navy = Color(0xFF6E452E)
val DeepTeal = Color(0xFF9A6540)
val Teal = Color(0xFFC98E5C)
val Aqua = Color(0xFFF2D2A9)
val Mist = Color(0xFFFFF8EE)
val Ink = Color(0xFF2F241D)
val SoftInk = Color(0xFF74645A)
val Success = Color(0xFF4E8B61)
val Warning = Color(0xFFD58B3C)
val Danger = Color(0xFFB94E42)

private val LightColors = lightColorScheme(
    primary = DeepTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF4DEC5),
    onPrimaryContainer = Ink,
    secondary = Teal,
    onSecondary = Color(0xFF3A261B),
    tertiary = Color(0xFFB56F3D),
    background = Mist,
    onBackground = Ink,
    surface = Color(0xFFFFFCF8),
    onSurface = Ink,
    surfaceVariant = Color(0xFFF3E8DC),
    onSurfaceVariant = SoftInk,
    outline = Color(0xFFD0BAA7)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE5B17D),
    onPrimary = Color(0xFF3A2416),
    primaryContainer = Color(0xFF6E452E),
    onPrimaryContainer = Color(0xFFFFE8D0),
    secondary = Color(0xFFD8A172),
    onSecondary = Color(0xFF321F15),
    background = Color(0xFF1D1713),
    onBackground = Color(0xFFF1E7DE),
    surface = Color(0xFF261E19),
    onSurface = Color(0xFFF1E7DE),
    surfaceVariant = Color(0xFF392D26),
    onSurfaceVariant = Color(0xFFD5C2B4),
    outline = Color(0xFF9A8170)
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
