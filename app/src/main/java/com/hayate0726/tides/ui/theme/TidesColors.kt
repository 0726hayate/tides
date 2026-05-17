package com.hayate0726.tides.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Color tokens per spec §6.
 *
 * Light theme: cream/clay (Material You–adjacent). One muted red accent.
 * Dark theme: true OLED black, monochrome with one accent red.
 *
 * All accent uses are guarded by a corresponding shape glyph (see Glyphs.kt)
 * so the design is CVD-safe.
 */
object TidesColors {
    // Light theme
    val LightBackground = Color(0xFFFAF5EC)
    val LightSurface = Color(0xFFEBE1CC)
    val LightInk = Color(0xFF1D1B16)
    val LightMutedInk = Color(0xFF6B6354)
    val LightFaintInk = Color(0xFFC7BDA6)
    val LightPeriodRed = Color(0xFFC25A3A)
    val LightAccentInk = Color(0xFF87502D)

    // Dark theme
    val DarkBackground = Color(0xFF0A0A0A)
    val DarkSurface = Color(0xFF18181B)
    val DarkInk = Color(0xFFFAFAFA)
    val DarkMutedInk = Color(0xFF71717A)
    val DarkFaintInk = Color(0xFF3F3F46)
    val DarkPeriodRed = Color(0xFFB8413A)
    val DarkAccentInk = Color(0xFFE57373)

    val LightScheme: ColorScheme = lightColorScheme(
        primary = LightInk,
        onPrimary = LightBackground,
        secondary = LightPeriodRed,
        onSecondary = LightBackground,
        background = LightBackground,
        onBackground = LightInk,
        surface = LightSurface,
        onSurface = LightInk,
        surfaceVariant = LightFaintInk,
        onSurfaceVariant = LightMutedInk,
        error = LightPeriodRed,
        onError = LightBackground,
    )

    val DarkScheme: ColorScheme = darkColorScheme(
        primary = DarkInk,
        onPrimary = DarkBackground,
        secondary = DarkPeriodRed,
        onSecondary = DarkInk,
        background = DarkBackground,
        onBackground = DarkInk,
        surface = DarkSurface,
        onSurface = DarkInk,
        surfaceVariant = DarkFaintInk,
        onSurfaceVariant = DarkMutedInk,
        error = DarkPeriodRed,
        onError = DarkInk,
    )
}
