package com.hayate0726.tides.ui.onboarding

import android.content.Context
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.hayate0726.tides.MainActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Drives the onboarding flow end-to-end through MainActivity's NavHost.
 *
 * The test does NOT use HiltAndroidRule — the project does not include
 * `hilt-android-testing` yet (deferred to a hardening pass). MainActivity
 * is @AndroidEntryPoint, so createAndroidComposeRule exercises the real
 * production Hilt graph. We delete the app's auth_meta.bin and tides.db
 * before/after each run so the flow always starts cold.
 */
class OnboardingFlowTest {

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetAppData() = clearFiles()

    @After
    fun cleanup() = clearFiles()

    private fun clearFiles() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        listOf("auth_meta.bin", "tides.db", "tides.db-shm", "tides.db-wal",
               "decoy.db", "decoy.db-shm", "decoy.db-wal").forEach {
            File(ctx.filesDir, it).delete()
        }
    }

    @Test
    fun complete_onboarding_lands_on_main() {
        rule.onNodeWithText("Continue").performClick()  // Welcome
        rule.onNodeWithText("Continue").performClick()  // Goals (defaults are ok)
        rule.onNodeWithText("PIN (6+ digits)").performTextInput("123456")
        rule.onNodeWithText("Confirm PIN").performTextInput("123456")
        rule.onNodeWithText("Continue").performClick()  // PIN setup
        rule.onNodeWithText("Continue").performClick()  // Biometric
        rule.onNodeWithText("Continue").performClick()  // Threat preset (default = LOCKED_WHEN_AWAY)
        rule.onNodeWithText("Skip").performClick()      // Last period

        // After complete() resolves (Argon2 derive + SQLCipher DB open), AppState
        // flips to Unlocked and MainHost renders. Its "Lock" button is the most
        // stable assertion target (calendar header is locale-sensitive).
        rule.waitUntil(timeoutMillis = 30_000) {
            rule.onAllNodes(hasText("Lock")).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Lock").assertExists()
    }
}
