package dz.latchi.ai.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ═══════ الهوية اللونية ═══════
val Ink   = Color(0xFF0A0E1A)
val Ink2  = Color(0xFF111726)
val Ink3  = Color(0xFF1A2136)
val Ink4  = Color(0xFF232B42)
val Line  = Color(0xFF1E2740)
val Line2 = Color(0xFF2C3752)
val Txt   = Color(0xFFE9EEF9)
val Txt2  = Color(0xFF9AA6BF)
val Txt3  = Color(0xFF5E6A85)
val Blue  = Color(0xFF3B82F6)
val Cyan  = Color(0xFF06B6D4)
val Violet= Color(0xFF8B5CF6)
val Green = Color(0xFF10B981)
val Amber = Color(0xFFF59E0B)
val Red   = Color(0xFFEF4444)

val BrandBrush = Brush.linearGradient(listOf(Blue, Violet))
val CyanBrush  = Brush.linearGradient(listOf(Cyan, Blue))
val HeroBrush  = Brush.verticalGradient(listOf(Color(0xFF1B2547), Ink))

private val DarkScheme = darkColorScheme(
    primary = Blue, onPrimary = Color.White,
    secondary = Cyan, onSecondary = Color.White,
    tertiary = Violet,
    background = Ink, onBackground = Txt,
    surface = Ink2, onSurface = Txt,
    surfaceVariant = Ink3, onSurfaceVariant = Txt2,
    outline = Line2, error = Red,
)

private val LightScheme = lightColorScheme(
    primary = Blue, onPrimary = Color.White,
    secondary = Cyan, tertiary = Violet,
    background = Color(0xFFF7F9FC), onBackground = Color(0xFF0E1420),
    surface = Color.White, onSurface = Color(0xFF0E1420),
    surfaceVariant = Color(0xFFEEF2F9), onSurfaceVariant = Color(0xFF4A5568),
    outline = Color(0xFFD6DEEA), error = Red,
)

private val AppTypography = Typography(
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 25.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 23.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.4.sp),
)

@Composable
fun LatchiTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = AppTypography,
        content = content,
    )
}

/** ألوان حسب الوضع الحالي */
object Pal {
    var dark: Boolean = true
    val bg get() = if (dark) Ink else Color(0xFFF7F9FC)
    val bg2 get() = if (dark) Ink2 else Color.White
    val bg3 get() = if (dark) Ink3 else Color(0xFFEEF2F9)
    val bg4 get() = if (dark) Ink4 else Color(0xFFE2E9F4)
    val line get() = if (dark) Line else Color(0xFFE1E7F0)
    val line2 get() = if (dark) Line2 else Color(0xFFCED6E4)
    val txt get() = if (dark) Txt else Color(0xFF0E1420)
    val txt2 get() = if (dark) Txt2 else Color(0xFF53607A)
    val txt3 get() = if (dark) Txt3 else Color(0xFF8894AB)
    val userBubble get() = if (dark) Ink3 else Color(0xFFEDF2FB)
}
