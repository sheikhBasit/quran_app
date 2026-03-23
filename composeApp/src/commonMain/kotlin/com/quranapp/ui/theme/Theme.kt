package com.quranapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.quranapp.Res
import com.quranapp.UthmanicHafs1Ver18

// ─── Colors ───────────────────────────────────────────────────────────────────

// Dark theme — deep greens and golds (Islamic aesthetic)
private val DarkColorScheme = darkColorScheme(
    primary          = Color(0xFF4CAF7D),   // Emerald green
    onPrimary        = Color(0xFF000000),
    primaryContainer = Color(0xFF1B5E3B),
    background       = Color(0xFF0D1117),   // Very dark
    surface          = Color(0xFF161B22),
    onBackground     = Color(0xFFE6EDF3),
    onSurface        = Color(0xFFE6EDF3),
    secondary        = Color(0xFFD4A848),   // Gold accent
    onSecondary      = Color(0xFF000000),
    error            = Color(0xFFCF6679),
)

// Light theme — cream/parchment (like a physical Mushaf)
private val LightColorScheme = lightColorScheme(
    primary          = Color(0xFF2D6A4F),   // Deep forest green
    onPrimary        = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFB7DFC9),
    background       = Color(0xFFF5F0E8),   // Cream/parchment
    surface          = Color(0xFFFFFFFF),
    onBackground     = Color(0xFF1A1A1A),
    onSurface        = Color(0xFF1A1A1A),
    secondary        = Color(0xFF8B5E3C),   // Warm brown accent
    onSecondary      = Color(0xFFFFFFFF),
    error            = Color(0xFFB00020),
)

// ─── Typography ───────────────────────────────────────────────────────────────

@OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
@Composable
fun getQuranFontFamily(): androidx.compose.ui.text.font.FontFamily {
    return androidx.compose.ui.text.font.FontFamily(
        org.jetbrains.compose.resources.Font(Res.font.UthmanicHafs1Ver18)
    )
}

private val QuranTypography = Typography(
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
)

// ─── Theme ────────────────────────────────────────────────────────────────────

@Composable
fun QuranAppTheme(
    themeMode: String = "system",
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = QuranTypography,
        content     = content,
    )
}
