package com.hayate0726.tides.ui.lock

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.hayate0726.tides.ui.theme.TidesTheme
import org.junit.Rule
import org.junit.Test

class LockScreenTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun keypad_renders_digits_zero_through_nine() {
        rule.setContent {
            TidesTheme {
                LockScreen(
                    pinLength = 0,
                    onDigit = {},
                    onBackspace = {},
                    onBiometric = null,
                    error = null,
                    cooldownExpiryEpochMs = null,
                )
            }
        }
        for (n in 0..9) rule.onNodeWithText(n.toString()).assertExists()
    }

    @Test
    fun tapping_a_digit_calls_onDigit() {
        val taps = mutableListOf<Int>()
        rule.setContent {
            TidesTheme {
                LockScreen(
                    pinLength = 0,
                    onDigit = { taps += it },
                    onBackspace = {},
                    onBiometric = null,
                    error = null,
                    cooldownExpiryEpochMs = null,
                )
            }
        }
        rule.onNodeWithText("5").performClick()
        rule.onNodeWithText("3").performClick()
        assert(taps == listOf(5, 3))
    }

    @Test
    fun error_state_shows_shake_message() {
        rule.setContent {
            TidesTheme {
                LockScreen(
                    pinLength = 0,
                    onDigit = {},
                    onBackspace = {},
                    onBiometric = null,
                    error = "Incorrect PIN",
                    cooldownExpiryEpochMs = null,
                )
            }
        }
        rule.onNodeWithText("Incorrect PIN").assertIsDisplayed()
    }
}
