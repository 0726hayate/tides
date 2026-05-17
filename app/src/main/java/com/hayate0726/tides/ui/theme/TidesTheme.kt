package com.hayate0726.tides.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * useDynamicColor defaults to FALSE so the CVD-safe palette in TidesColors
 * actually renders. The colors are part of the app's identity and the result
 * of explicit accessibility research (see /mockups/direction-2-materialyou
 * and the CVD simulations there). Dynamic Material You theming on Android
 * 12+ would silently override that work. A Settings toggle for users who
 * prefer dynamic color lands later.
 */
@Composable
fun TidesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val ctx = LocalContext.current
    val scheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        darkTheme -> TidesColors.DarkScheme
        else -> TidesColors.LightScheme
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = TidesTypography.Default,
        shapes = TidesShapes.Default,
        content = content,
    )
}
