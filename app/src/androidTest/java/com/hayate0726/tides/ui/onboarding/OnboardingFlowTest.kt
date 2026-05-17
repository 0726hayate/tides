package com.hayate0726.tides.ui.onboarding

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.hayate0726.tides.MainActivity
import org.junit.Rule
import org.junit.Test

/**
 * Drives the onboarding flow end-to-end through MainActivity's NavHost.
 *
 * NOTE: This test does not use HiltAndroidRule because the project does not
 * currently include `hilt-android-testing` / a custom HiltTestRunner (that
 * setup is deferred to a later wave). MainActivity is already
 * `@AndroidEntryPoint`, so launching it via `createAndroidComposeRule`
 * exercises the real Hilt graph from the production Application.
 */
class OnboardingFlowTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun complete_onboarding_lands_on_calendar() {
        rule.onNodeWithText("Continue").performClick()  // Welcome
        rule.onNodeWithText("Continue").performClick()  // Goals (defaults are ok)
        rule.onNodeWithText("PIN (6+ digits)").performTextInput("123456")
        rule.onNodeWithText("Confirm PIN").performTextInput("123456")
        rule.onNodeWithText("Continue").performClick()  // PIN setup
        rule.onNodeWithText("Continue").performClick()  // Biometric
        rule.onNodeWithText("Continue").performClick()  // Threat preset (default = LOCKED_WHEN_AWAY)
        rule.onNodeWithText("Skip").performClick()      // Last period

        // After completion, the calendar header should be visible
        rule.onNodeWithText("Cycle day").assertExists()
    }
}
