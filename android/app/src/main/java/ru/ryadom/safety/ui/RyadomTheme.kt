package ru.ryadom.safety.ui

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Navy = Color(0xFF102A36)
val DeepTeal = Color(0xFF195E68)
val Teal = Color(0xFF2A8C91)
val Aqua = Color(0xFF78C7C3)
val Mist = Color(0xFFF3F7F8)
val Ink = Color(0xFF142329)
val SoftInk = Color(0xFF5C6B70)
val Success = Color(0xFF2F8F63)
val Warning = Color(0xFFE2A84A)
val Danger = Color(0xFFC94C55)

private val LightColors = lightColorScheme(
    primary = DeepTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4F0F0),
    onPrimaryContainer = Navy,
    secondary = Teal,
    onSecondary = Color.White,
    tertiary = Color(0xFFB67A2B),
    background = Mist,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE7EFF1),
    onSurfaceVariant = SoftInk,
    outline = Color(0xFFB7C5C8)
)

private val DarkColors = darkColorScheme(
    primary = Aqua,
    onPrimary = Color(0xFF00363A),
    primaryContainer = Color(0xFF0E4F57),
    onPrimaryContainer = Color(0xFFC7F2F0),
    secondary = Color(0xFF78C7C3),
    onSecondary = Color(0xFF003736),
    background = Color(0xFF0E171B),
    onBackground = Color(0xFFE1EAEC),
    surface = Color(0xFF152126),
    onSurface = Color(0xFFE1EAEC),
    surfaceVariant = Color(0xFF223137),
    onSurfaceVariant = Color(0xFFBBC9CC),
    outline = Color(0xFF83969A)
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
