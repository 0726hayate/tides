package com.hayate0726.tides.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.hayate0726.tides.domain.model.CalendarView
import com.hayate0726.tides.ui.theme.TidesColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class CalendarMonthState(
    val month: YearMonth,
    val today: LocalDate,
    val periodDays: Set<LocalDate>,
    /** Predicted future period day ranges (current cycle + projected cycles). */
    val predictedPeriodRanges: List<ClosedRange<LocalDate>>,
    /** Predicted ovulation windows (current cycle + projected cycles). */
    val ovulationRanges: List<ClosedRange<LocalDate>>,
    /** Predicted follicular ranges (after period, before ovulation). */
    val follicularRanges: List<ClosedRange<LocalDate>> = emptyList(),
    /** Predicted luteal ranges (after ovulation, before next period). */
    val lutealRanges: List<ClosedRange<LocalDate>> = emptyList(),
    val symptomDays: Set<LocalDate>,
)

private fun List<ClosedRange<LocalDate>>.containsDate(d: LocalDate): Boolean =
    any { d in it }

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

    val periodRing = MaterialTheme.colorScheme.secondary
    val dark = isSystemInDarkTheme()
    val ovulationRing = if (dark) TidesColors.DarkOvulationAccent else TidesColors.LightOvulationAccent
    val follicularBar = if (dark) TidesColors.DarkFollicularAccent else TidesColors.LightFollicularAccent
    val lutealBar = if (dark) TidesColors.DarkLutealAccent else TidesColors.LightLutealAccent

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
                        val isPeriod = view != CalendarView.SYMPTOMS && date in state.periodDays
                        // Dotted period ring is suppressed on days already
                        // shown as solid logged-period markers — otherwise
                        // the ring just doubles up on the filled circle.
                        val isPredictedPeriod = !isPeriod &&
                            view != CalendarView.SYMPTOMS &&
                            state.predictedPeriodRanges.containsDate(date)
                        val isOvulation = view in listOf(CalendarView.ALL, CalendarView.PHASES) &&
                            state.ovulationRanges.containsDate(date)
                        val phasesVisible = view in listOf(CalendarView.ALL, CalendarView.PHASES)
                        val isFollicular = phasesVisible && state.follicularRanges.containsDate(date)
                        val isLuteal = phasesVisible && state.lutealRanges.containsDate(date)
                        DayCell(
                            date = date,
                            isToday = date == state.today,
                            isPeriod = isPeriod,
                            isPredictedPeriod = isPredictedPeriod,
                            isOvulation = isOvulation,
                            isFollicular = isFollicular,
                            isLuteal = isLuteal,
                            hasSymptom = view in listOf(CalendarView.ALL, CalendarView.SYMPTOMS) &&
                                date in state.symptomDays,
                            periodRingColor = periodRing,
                            ovulationRingColor = ovulationRing,
                            follicularBarColor = follicularBar,
                            lutealBarColor = lutealBar,
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
    isFollicular: Boolean,
    isLuteal: Boolean,
    hasSymptom: Boolean,
    periodRingColor: Color,
    ovulationRingColor: Color,
    follicularBarColor: Color,
    lutealBarColor: Color,
    modifier: Modifier = Modifier,
) {
    // Show the follicular/luteal bar only on days that don't already carry a
    // period or ovulation marker — otherwise the cell stacks too many cues.
    val phaseBarColor: Color? = when {
        isPeriod || isPredictedPeriod || isOvulation -> null
        isFollicular -> follicularBarColor
        isLuteal -> lutealBarColor
        else -> null
    }
    Box(modifier = modifier.semantics(mergeDescendants = true) {
        val parts = buildList {
            add(date.format(java.time.format.DateTimeFormatter.ofPattern("MMMM d")))
            if (isPeriod) add("period day")
            if (isPredictedPeriod) add("predicted period")
            if (isOvulation) add("ovulation")
            if (phaseBarColor != null && isFollicular) add("follicular")
            if (phaseBarColor != null && isLuteal) add("luteal")
            if (hasSymptom) add("symptom logged")
        }
        contentDescription = parts.joinToString(", ")
    }, contentAlignment = Alignment.Center) {
        // Layered dotted rings — period (red) is the outer ring and
        // ovulation (blue) goes inside so both stay visible on days that
        // overlap (rare with a 28-day cycle but possible at edges).
        val ringModifier = Modifier
            .size(36.dp)
            .let {
                if (isPredictedPeriod) it.dottedRing(periodRingColor, insetDp = 1.5f) else it
            }
            .let {
                if (isOvulation && !isPeriod) it.dottedRing(ovulationRingColor, insetDp = if (isPredictedPeriod) 4.5f else 1.5f)
                else it
            }

        Box(
            modifier = ringModifier
                .background(
                    color = when {
                        isPeriod -> MaterialTheme.colorScheme.secondary
                        isToday -> MaterialTheme.colorScheme.onSurface
                        else -> Color.Transparent
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
        if (phaseBarColor != null) {
            // Thin colored bar pinned to the bottom of the cell — sits below
            // the day number and the symptom dot.
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .padding(top = 31.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Box(modifier = Modifier
                    .size(width = 14.dp, height = 2.dp)
                    .background(phaseBarColor))
            }
        }
    }
}

/**
 * Draws a dotted circular ring around the cell, inset by [insetDp] so
 * multiple rings on the same cell don't overlap. Stroke width 1.5 dp, dash
 * pattern (3 px on, 3 px off) — reads as a clear "expected, not logged"
 * marker without competing with solid logged-period circles.
 */
private fun Modifier.dottedRing(color: Color, insetDp: Float): Modifier = this.drawBehind {
    val strokePx = 1.5.dp.toPx()
    val inset = insetDp.dp.toPx()
    val radius = (size.minDimension / 2f) - inset - strokePx / 2f
    if (radius <= 0f) return@drawBehind
    drawCircle(
        color = color,
        radius = radius,
        center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f),
        style = Stroke(
            width = strokePx,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()), 0f),
        ),
    )
}
