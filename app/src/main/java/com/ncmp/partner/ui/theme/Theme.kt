package com.ncmp.partner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** 液态玻璃主题色板 */
object GlassPalette {
    val Ink = Color(0xFF07070C)
    val InkSoft = Color(0xFF12121A)
    val Accent = Color(0xFFEC4141)
    val AccentSoft = Color(0xFFFF6B6B)
    val Violet = Color(0xFF7A5CFF)
    val Cyan = Color(0xFF00C2C7)
    val Amber = Color(0xFFFFA53D)
    val Mint = Color(0xFF3DDC97)

    val TextPrimary = Color(0xFFF2F3F7)
    val TextSecondary = Color(0xB3F2F3F7)
    val TextTertiary = Color(0x80F2F3F7)

    // 玻璃层
    val GlassFillTop = Color(0x24FFFFFF)
    val GlassFillBottom = Color(0x0AFFFFFF)
    val GlassBorderTop = Color(0x59FFFFFF)
    val GlassBorderBottom = Color(0x14FFFFFF)
    val GlassHighlight = Color(0x1FFFFFFF)

    val Success = Color(0xFF4CD07D)
    val Warning = Color(0xFFFFC061)
    val Error = Color(0xFFFF6B6B)
}

private val DarkScheme = darkColorScheme(
    primary = GlassPalette.Accent,
    onPrimary = Color.White,
    secondary = GlassPalette.Violet,
    background = GlassPalette.Ink,
    surface = GlassPalette.InkSoft,
    onBackground = GlassPalette.TextPrimary,
    onSurface = GlassPalette.TextPrimary,
    error = GlassPalette.Error,
)

private val NcmpTypography = Typography(
    displaySmall = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp),
    bodyMedium = TextStyle(fontSize = 13.5.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun NcmpTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_EXPRESSION")
    isSystemInDarkTheme() // 始终使用深色玻璃主题
    MaterialTheme(
        colorScheme = DarkScheme,
        typography = NcmpTypography,
        content = content,
    )
}
