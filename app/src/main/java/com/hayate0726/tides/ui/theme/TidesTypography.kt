package com.hayate0726.tides.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object TidesTypography {
    private val sans = FontFamily.SansSerif

    val Default = Typography(
        displayLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, letterSpacing = (-0.02).sp),
        displayMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, letterSpacing = (-0.02).sp),
        headlineLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 26.sp, letterSpacing = (-0.01).sp),
        headlineMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 22.sp),
        titleLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 18.sp),
        titleMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Medium, fontSize = 16.sp),
        bodyLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
        labelLarge = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.1.sp),
        labelMedium = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.08.sp),
        labelSmall = TextStyle(fontFamily = sans, fontWeight = FontWeight.SemiBold, fontSize = 10.sp, letterSpacing = 0.06.sp),
    )
}
