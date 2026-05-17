package com.hayate0726.tides.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hayate0726.tides.domain.model.CalendarView
import com.hayate0726.tides.ui.theme.DiamondGlyph
import com.hayate0726.tides.ui.theme.DropGlyph
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class CalendarMonthState(
    val month: YearMonth,
    val today: LocalDate,
    val periodDays: Set<LocalDate>,
    val predictedPeriod: ClosedRange<LocalDate>?,
    val ovulationWindow: ClosedRange<LocalDate>?,
    val symptomDays: Set<LocalDate>,
)

@Composable
fun CalendarMonth(
    state: CalendarMonthState,
    view: CalendarView,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstOfMonth = state.month.atDay(1)
    val daysInMonth = state.month.lengthOfMonth()
    val firstDayOfWeek = firstOfMonth.dayOfWeek
    val leadingBlanks = (firstDayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7

    Column(modifier = modifier) {
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                Text(
                    d,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        var dayIndex = 0
        val totalCells = leadingBlanks + daysInMonth
        val rows = (totalCells + 6) / 7
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    if (cellIndex < leadingBlanks || dayIndex >= daysInMonth) {
                        Box(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = firstOfMonth.plusDays(dayIndex.toLong())
                        DayCell(
                            date = date,
                            isToday = date == state.today,
                            isPeriod = view != CalendarView.SYMPTOMS && date in state.periodDays,
                            isPredictedPeriod = view != CalendarView.SYMPTOMS &&
                                state.predictedPeriod?.contains(date) == true &&
                                date !in state.periodDays,
                            isOvulation = view in listOf(CalendarView.ALL, CalendarView.PHASES) &&
                                state.ovulationWindow?.contains(date) == true,
                            hasSymptom = view in listOf(CalendarView.ALL, CalendarView.SYMPTOMS) &&
                                date in state.symptomDays,
                            modifier = Modifier.weight(1f).aspectRatio(1f).clickable { onDayClick(date) },
                        )
                        dayIndex++
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean,
    isPeriod: Boolean,
    isPredictedPeriod: Boolean,
    isOvulation: Boolean,
    hasSymptom: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = when {
                        isPeriod -> MaterialTheme.colorScheme.secondary
                        isToday -> MaterialTheme.colorScheme.onSurface
                        else -> androidx.compose.ui.graphics.Color.Transparent
                    },
                    shape = CircleShape,
                )
                .padding(2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                color = when {
                    isPeriod -> MaterialTheme.colorScheme.onSecondary
                    isToday -> MaterialTheme.colorScheme.background
                    else -> MaterialTheme.colorScheme.onBackground
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (isPeriod) {
                Box(modifier = Modifier.size(28.dp).padding(2.dp), contentAlignment = Alignment.TopEnd) {
                    DropGlyph(color = MaterialTheme.colorScheme.onSecondary, size = 5.dp)
                }
            }
            if (isPredictedPeriod) {
                Box(modifier = Modifier.size(28.dp).padding(2.dp), contentAlignment = Alignment.TopEnd) {
                    DropGlyph(
                        color = MaterialTheme.colorScheme.secondary,
                        size = 5.dp,
                        filled = false,
                    )
                }
            }
            if (isOvulation) {
                Box(modifier = Modifier.size(28.dp).padding(2.dp), contentAlignment = Alignment.TopEnd) {
                    DiamondGlyph(color = MaterialTheme.colorScheme.onBackground, size = 5.dp)
                }
            }
        }
        if (hasSymptom) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .padding(top = 26.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(modifier = Modifier
                    .size(4.dp)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape))
            }
        }
    }
}
