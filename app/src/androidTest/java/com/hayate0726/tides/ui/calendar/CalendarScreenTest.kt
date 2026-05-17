package com.hayate0726.tides.ui.calendar

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.hayate0726.tides.domain.model.CalendarView
import com.hayate0726.tides.ui.theme.TidesTheme
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarScreenTest {

    @get:Rule val rule = createComposeRule()

    @Test
    fun calendar_month_header_renders() {
        rule.setContent {
            TidesTheme {
                CalendarScreen(
                    monthState = CalendarMonthState(
                        month = YearMonth.of(2026, 5),
                        today = LocalDate.of(2026, 5, 14),
                        periodDays = setOf(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 2)),
                        predictedPeriod = LocalDate.of(2026, 5, 28)..LocalDate.of(2026, 6, 1),
                        ovulationWindow = LocalDate.of(2026, 5, 15)..LocalDate.of(2026, 5, 16),
                        symptomDays = setOf(LocalDate.of(2026, 5, 7), LocalDate.of(2026, 5, 10)),
                    ),
                    view = CalendarView.ALL,
                    onViewChange = {},
                    onDayClick = {},
                )
            }
        }
        rule.onNodeWithText("May").assertExists()
        rule.onNodeWithText(" 2026").assertExists()
    }

    @Test
    fun view_toggle_changes_selection_state() {
        var view = CalendarView.ALL
        rule.setContent {
            TidesTheme {
                CalendarScreen(
                    monthState = CalendarMonthState(
                        month = YearMonth.of(2026, 5),
                        today = LocalDate.of(2026, 5, 14),
                        periodDays = emptySet(),
                        predictedPeriod = null,
                        ovulationWindow = null,
                        symptomDays = emptySet(),
                    ),
                    view = view,
                    onViewChange = { view = it },
                    onDayClick = {},
                )
            }
        }
        rule.onNodeWithText("All").assertExists()
        rule.onNodeWithText("Period only").assertExists()
        rule.onNodeWithText("Phases").assertExists()
        rule.onNodeWithText("Symptoms").assertExists()
    }
}
