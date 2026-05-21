package com.hayate0726.tides.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hayate0726.tides.domain.model.CalendarView
import com.hayate0726.tides.ui.calendar.CalendarMonth
import com.hayate0726.tides.ui.calendar.CalendarMonthState
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun PredictionPreviewScreen(
    state: PredictionPreviewState,
    onEdit: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Here's your first prediction", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.size(8.dp))
        Text(
            "Based on a 28-day default. It'll get sharper as you log more.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(20.dp))

        val month = YearMonth.from(state.enteredPeriod.start)
        val calendarState = CalendarMonthState(
            month = month,
            today = state.enteredPeriod.start,
            periodDays = daysIn(state.enteredPeriod),
            predictedPeriod = state.predictedNextPeriod,
            ovulationWindow = state.fertileWindow,
            symptomDays = emptySet(),
        )
        CalendarMonth(
            state = calendarState,
            view = CalendarView.ALL,
            onDayClick = { /* read-only preview */ },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(16.dp))

        val fmt = DateTimeFormatter.ofPattern("MMM d")
        Text(
            "Next period: ~ ${state.predictedNextPeriod.start.format(fmt)}",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (state.fertileWindow != null) {
            Text(
                "Fertile window: ~ ${state.fertileWindow.start.format(fmt)} – " +
                    state.fertileWindow.endInclusive.format(fmt),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (state.hormonalBcNote) {
            Spacer(Modifier.size(12.dp))
            Text(
                "Hormonal birth control can change your cycle. Predictions adjust as you log.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("Edit") }
            Button(onClick = onConfirm, modifier = Modifier.weight(1f)) { Text("Looks good") }
        }
    }
}

private fun daysIn(range: ClosedRange<LocalDate>): Set<LocalDate> {
    val out = mutableSetOf<LocalDate>()
    var d = range.start
    while (!d.isAfter(range.endInclusive)) {
        out += d
        d = d.plusDays(1)
    }
    return out
}
