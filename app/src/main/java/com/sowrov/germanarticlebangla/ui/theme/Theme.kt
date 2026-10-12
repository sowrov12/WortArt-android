package com.sowrov.germanarticlebangla.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.sowrov.germanarticlebangla.data.Article

@Immutable
class WortColors(
    val isDark: Boolean,
    val background: Color,
    val foreground: Color,
    val card: Color,
    val primary: Color,
    val primaryForeground: Color,
    val secondary: Color,
    val mutedForeground: Color,
    val accent: Color,
    val destructive: Color,
    val success: Color,
    val der: Color,
    val die: Color,
    val das: Color,
    val border: Color,
) {
    fun article(article: Article): Color = when (article) {
        Article.der -> der
        Article.die -> die
        Article.das -> das
    }
}

val LightColors = WortColors(
    isDark = false,
    background = Color(0xFFFBF6EF),
    foreground = Color(0xFF221812),
    card = Color(0xFFFFFDFA),
    primary = Color(0xFFB45600),
    primaryForeground = Color(0xFFFEFBF8),
    secondary = Color(0xFFF0EAE2),
    mutedForeground = Color(0xFF63554A),
    accent = Color(0xFFEBE4D8),
    destructive = Color(0xFFCC2827),
    success = Color(0xFF007834),
    der = Color(0xFF0068AD),
    die = Color(0xFFC62C58),
    das = Color(0xFF8C5F00),
    border = Color(0x1A000000),
)

val DarkColors = WortColors(
    isDark = true,
    background = Color(0xFF140E0A),
    foreground = Color(0xFFF7F1E7),
    card = Color(0xFF1F1813),
    primary = Color(0xFFF4A34B),
    primaryForeground = Color(0xFF1D140D),
    secondary = Color(0xFF2A221D),
    mutedForeground = Color(0xFFAFA294),
    accent = Color(0xFF332922),
    destructive = Color(0xFFF75D59),
    success = Color(0xFF5AC576),
    der = Color(0xFF65B4E9),
    die = Color(0xFFF7758A),
    das = Color(0xFFEEC743),
    border = Color(0x14FFFFFF),
)

val LocalWortColors = staticCompositionLocalOf { DarkColors }

object WortTheme {
    val colors: WortColors
        @Composable
        get() = LocalWortColors.current
}

val SerifFont: FontFamily = FontFamily.Serif

val CardShape = RoundedCornerShape(24.dp)
val SmallShape = RoundedCornerShape(16.dp)

@Composable
fun WortArtTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.primaryForeground,
            background = colors.background,
            onBackground = colors.foreground,
            surface = colors.card,
            onSurface = colors.foreground,
            surfaceVariant = colors.secondary,
            onSurfaceVariant = colors.mutedForeground,
            error = colors.destructive,
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.primaryForeground,
            background = colors.background,
            onBackground = colors.foreground,
            surface = colors.card,
            onSurface = colors.foreground,
            surfaceVariant = colors.secondary,
            onSurfaceVariant = colors.mutedForeground,
            error = colors.destructive,
        )
    }
    CompositionLocalProvider(LocalWortColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
