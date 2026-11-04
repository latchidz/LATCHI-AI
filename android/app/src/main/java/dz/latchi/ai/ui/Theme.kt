package dz.latchi.ai.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Violet = Color(0xFF7C5CFF)
val VioletDeep = Color(0xFF5B3FD9)
val Cyan = Color(0xFF22D3EE)
val Pink = Color(0xFFF472B6)
val BgDeep = Color(0xFF07080F)
val BgSoft = Color(0xFF0D0F1A)
val CardBg = Color(0xFF141727)
val CardHi = Color(0xFF1C2036)
val TextMain = Color(0xFFEDEFF7)
val TextMuted = Color(0xFF8A90A8)
val OkGreen = Color(0xFF34D399)
val ErrRed = Color(0xFFFF6B81)
val Gold = Color(0xFFF5C451)

val BrandBrush = Brush.linearGradient(listOf(Violet, Cyan))
val UserBrush = Brush.linearGradient(listOf(VioletDeep, Violet))
val BgBrush = Brush.verticalGradient(listOf(Color(0xFF0B0D18), BgDeep, Color(0xFF0A0B12)))

private val Scheme = darkColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    secondary = Cyan,
    onSecondary = Color(0xFF001318),
    tertiary = Pink,
    background = BgDeep,
    onBackground = TextMain,
    surface = CardBg,
    onSurface = TextMain,
    surfaceVariant = CardHi,
    onSurfaceVariant = TextMuted,
    error = ErrRed,
)

@Composable
fun LatchiAITheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
