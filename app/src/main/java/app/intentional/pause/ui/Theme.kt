package app.intentional.pause.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Mint = Color(0xFFB9F3DC)
val DeepTeal = Color(0xFF123E39)
val AlertRed = Color(0xFFFF625F)
private val Light = lightColorScheme(
    primary = Color(0xFF176B60), onPrimary = Color.White,
    primaryContainer = Color(0xFFD3EFE3), onPrimaryContainer = DeepTeal,
    secondary = Color(0xFF49665B), secondaryContainer = Color(0xFFD3EFE3), onSecondaryContainer = DeepTeal,
    background = Color(0xFFF5F7F3), onBackground = Color(0xFF182C29),
    surface = Color(0xFFF5F7F3), onSurface = Color(0xFF182C29),
    surfaceContainer = Color(0xFFEBF0EA), surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFE6ECE4), surfaceContainerHighest = Color(0xFFDFE7DC),
    surfaceVariant = Color(0xFFE4EBE3), onSurfaceVariant = Color(0xFF566761),
    outline = Color(0xFF7D8B84), outlineVariant = Color(0xFFD6DFD6),
)
private val Dark = darkColorScheme(
    primary = Mint, onPrimary = DeepTeal,
    primaryContainer = Color(0xFF204E45), onPrimaryContainer = Mint,
    secondary = Color(0xFFB4CDBF), secondaryContainer = Color(0xFF2A4C3F), onSecondaryContainer = Mint,
    background = Color(0xFF101B19), onBackground = Color(0xFFE6EEE8),
    surface = Color(0xFF101B19), onSurface = Color(0xFFE6EEE8),
    surfaceContainer = Color(0xFF1D2B27), surfaceContainerLow = Color(0xFF192722),
    surfaceContainerHigh = Color(0xFF24382E), surfaceContainerHighest = Color(0xFF2B4034),
    surfaceVariant = Color(0xFF2B3B34), onSurfaceVariant = Color(0xFFB4C5BB),
    outline = Color(0xFF82988A), outlineVariant = Color(0xFF33473B),
)
private val Type = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 48.sp, lineHeight = 54.sp, letterSpacing = (-2).sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 27.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 28.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
)

@Composable fun IntentTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, typography = Type, content = content)
}
