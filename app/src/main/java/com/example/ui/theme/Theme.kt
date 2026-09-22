package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

fun getThemeColorScheme(themeName: String) = when (themeName.lowercase()) {
    "ocean" -> darkColorScheme(
        primary = OceanPrimary,
        background = OceanBg,
        surface = OceanSurface,
        surfaceVariant = Color(0xFF13283E),
        onPrimary = Color.Black,
        onBackground = TextPrimary,
        onSurface = TextPrimary
    )
    "midnight" -> darkColorScheme(
        primary = MidnightPrimary,
        background = MidnightBg,
        surface = MidnightSurface,
        surfaceVariant = Color(0xFF1E2238),
        onPrimary = Color.White,
        onBackground = TextPrimary,
        onSurface = TextPrimary
    )
    "soft" -> darkColorScheme(
        primary = SoftPrimary,
        background = SoftBg,
        surface = SoftSurface,
        surfaceVariant = Color(0xFF332435),
        onPrimary = Color.Black,
        onBackground = TextPrimary,
        onSurface = TextPrimary
    )
    "classic" -> darkColorScheme(
        primary = ClassicPrimary,
        background = ClassicBg,
        surface = ClassicSurface,
        surfaceVariant = Color(0xFF262626),
        onPrimary = Color.Black,
        onBackground = TextPrimary,
        onSurface = TextPrimary
    )
    "gunmetal" -> darkColorScheme(
        primary = GunmetalPrimary,
        background = GunmetalBg,
        surface = GunmetalSurface,
        surfaceVariant = Color(0xFF283141),
        onPrimary = Color.Black,
        onBackground = TextPrimary,
        onSurface = TextPrimary
    )
    "lavender" -> darkColorScheme(
        primary = LavenderPrimary,
        background = LavenderBg,
        surface = LavenderSurface,
        surfaceVariant = Color(0xFF2D2345),
        onPrimary = Color.Black,
        onBackground = TextPrimary,
        onSurface = TextPrimary
    )
    "purple" -> darkColorScheme(
        primary = Color(0xFFA855F7),
        background = Color(0xFF090614),
        surface = Color(0xFF150F2B),
        surfaceVariant = Color(0xFF251A48),
        onPrimary = Color.White,
        onBackground = TextPrimary,
        onSurface = TextPrimary
    )
    else -> darkColorScheme(
        primary = AccentPurple,
        background = DarkBg,
        surface = DarkSurface,
        surfaceVariant = DarkSurfaceVariant,
        onPrimary = Color.White,
        onBackground = TextPrimary,
        onSurface = TextPrimary
    )
}

@Composable
fun AetherTheme(
    selectedTheme: String = "default",
    content: @Composable () -> Unit
) {
    val colorScheme = getThemeColorScheme(selectedTheme)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
