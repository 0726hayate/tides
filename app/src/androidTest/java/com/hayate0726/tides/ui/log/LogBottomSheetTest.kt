package com.hayate0726.tides.ui.log

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.hayate0726.tides.ui.theme.TidesTheme
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class LogBottomSheetTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun flow_pills_render_all_intensities() {
        rule.setContent {
            TidesTheme {
                LogBottomSheet(
                    date = LocalDate.of(2026, 5, 14),
                    cycleDay = 14,
                    initialFlow = null,
                    initialSymptoms = emptyMap(),
                    initialNote = "",
                    onSave = { _, _, _ -> },
                    onCancel = {},
                )
            }
        }
        rule.onNodeWithText("None").assertExists()
        rule.onNodeWithText("Spotting").assertExists()
        rule.onNodeWithText("Light").assertExists()
        rule.onNodeWithText("Medium").assertExists()
        rule.onNodeWithText("Heavy").assertExists()
    }
}
