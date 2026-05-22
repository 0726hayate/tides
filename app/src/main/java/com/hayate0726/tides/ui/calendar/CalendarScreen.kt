package com.hayate0726.tides.ui.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hayate0726.tides.domain.model.CalendarView
import java.time.LocalDate

@Composable
fun CalendarScreen(
    monthState: CalendarMonthState,
    view: CalendarView,
    onViewChange: (CalendarView) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onGoToToday: () -> Unit = {},
) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        MonthHeader(
            month = monthState.month,
            today = monthState.today,
            onPrevious = onPreviousMonth,
            onNext = onNextMonth,
            onGoToToday = onGoToToday,
        )
        Spacer(Modifier.size(20.dp))
        CalendarViewToggle(current = view, onChange = onViewChange)
        Spacer(Modifier.size(16.dp))
        CalendarMonth(
            state = monthState,
            view = view,
            onDayClick = onDayClick,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(16.dp))
        // showOvulation is implicit: if any ovulation range is being passed in,
        // the user is allowed to see ovulation, so the legend mirrors that.
        CalendarLegend(
            showOvulation = monthState.ovulationRanges.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MonthHeader(
    month: java.time.YearMonth,
    today: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onGoToToday: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.semantics { contentDescription = "Previous month" },
        ) { Text("‹", style = MaterialTheme.typography.headlineMedium) }

        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    month.month.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    " ${month.year}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val viewingToday = month == java.time.YearMonth.from(today)
            if (!viewingToday) {
                Surface(
                    onClick = onGoToToday,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(
                        "Today",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }

        IconButton(
            onClick = onNext,
            modifier = Modifier.semantics { contentDescription = "Next month" },
        ) { Text("›", style = MaterialTheme.typography.headlineMedium) }
    }
}
