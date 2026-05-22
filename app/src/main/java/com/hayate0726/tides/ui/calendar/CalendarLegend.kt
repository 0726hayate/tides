package com.hayate0726.tides.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.dp
import com.hayate0726.tides.ui.theme.TidesColors

/**
 * Compact legend for the calendar markers — placed directly under the
 * month grid so first-time users can decode the dotted rings and phase
 * bars without trial and error.
 */
@Composable
fun CalendarLegend(
    showOvulation: Boolean,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val periodColor = MaterialTheme.colorScheme.secondary
    val ovulationColor = if (dark) TidesColors.DarkOvulationAccent else TidesColors.LightOvulationAccent
    val follicularColor = if (dark) TidesColors.DarkFollicularAccent else TidesColors.LightFollicularAccent
    val lutealColor = if (dark) TidesColors.DarkLutealAccent else TidesColors.LightLutealAccent

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendItem(swatch = { SolidDot(periodColor) }, label = "Period")
            LegendItem(swatch = { DottedRing(periodColor) }, label = "Predicted")
            if (showOvulation) {
                LegendItem(swatch = { DottedRing(ovulationColor) }, label = "Ovulation")
            }
        }
        Spacer(Modifier.size(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendItem(swatch = { PhaseBar(follicularColor) }, label = "Follicular")
            LegendItem(swatch = { PhaseBar(lutealColor) }, label = "Luteal")
            LegendItem(
                swatch = {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape),
                    )
                },
                label = "Symptom",
            )
        }
    }
}

@Composable
private fun LegendItem(swatch: @Composable () -> Unit, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
        Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) { swatch() }
        Spacer(Modifier.size(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SolidDot(color: Color) {
    Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
}

@Composable
private fun DottedRing(color: Color) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .drawBehind {
                val strokePx = 1.5.dp.toPx()
                val radius = (size.minDimension / 2f) - strokePx / 2f
                drawCircle(
                    color = color,
                    radius = radius,
                    style = Stroke(
                        width = strokePx,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(3.dp.toPx(), 3.dp.toPx()), 0f,
                        ),
                    ),
                )
            },
    )
}

@Composable
private fun PhaseBar(color: Color) {
    Box(modifier = Modifier.size(width = 14.dp, height = 2.dp).background(color))
}
