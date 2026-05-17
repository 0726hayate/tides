package com.hayate0726.tides.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TidesColorsTest {

    /**
     * Period/ovulation marks are small graphic objects (drops, diamonds, bars),
     * not body text. WCAG 2.1 SC 1.4.11 "Non-text Contrast" sets the floor at
     * 3:1 for such UI components and graphical objects. The 4.5:1 body-text
     * threshold doesn't apply here — and the CVD-safe palette deliberately
     * uses warmer reds for the calendar marks. Shape redundancy (DropGlyph,
     * DiamondGlyph, DashedBar in Glyphs.kt) carries the meaning for CVD users
     * regardless of contrast ratio.
     */
    private val MIN_GRAPHIC_CONTRAST = 3.0

    @Test
    fun light_period_red_contrasts_against_cream_bg() {
        val ratio = contrastRatio(TidesColors.LightPeriodRed, TidesColors.LightBackground)
        assertTrue(
            ratio >= MIN_GRAPHIC_CONTRAST,
            "WCAG 1.4.11 requires >= $MIN_GRAPHIC_CONTRAST for graphics; got $ratio",
        )
    }

    @Test
    fun dark_period_red_contrasts_against_black_bg() {
        val ratio = contrastRatio(TidesColors.DarkPeriodRed, TidesColors.DarkBackground)
        assertTrue(
            ratio >= MIN_GRAPHIC_CONTRAST,
            "WCAG 1.4.11 requires >= $MIN_GRAPHIC_CONTRAST for graphics; got $ratio",
        )
    }

    @Test
    fun dark_today_white_is_max_contrast_against_black_bg() {
        val ratio = contrastRatio(Color.White, TidesColors.DarkBackground)
        assertTrue(ratio >= 15.0, "expected near-max contrast for today indicator")
    }

    private fun contrastRatio(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        val lighter = maxOf(la, lb)
        val darker = minOf(la, lb)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun luminance(c: Color): Double {
        fun ch(v: Float): Double {
            val s = v.toDouble()
            return if (s <= 0.03928) s / 12.92
            else Math.pow((s + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * ch(c.red) + 0.7152 * ch(c.green) + 0.0722 * ch(c.blue)
    }
}
