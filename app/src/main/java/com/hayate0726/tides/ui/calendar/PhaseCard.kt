package com.hayate0726.tides.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hayate0726.tides.domain.model.Phase

@Composable
fun PhaseCard(
    currentPhase: Phase,
    cycleDay: Int,
    nextPeriodLabel: String?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "CURRENT PHASE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${phaseLabel(currentPhase)} likely",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(cycleDay.toString(), style = MaterialTheme.typography.displayMedium)
                    Text(
                        "cycle day",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.size(16.dp))
            PhaseProgressBar(currentPhase)

            if (nextPeriodLabel != null) {
                Spacer(Modifier.size(12.dp))
                Text(
                    "Next period likely $nextPeriodLabel",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PhaseProgressBar(current: Phase) {
    Row(Modifier.fillMaxWidth()) {
        listOf(
            Phase.MENSTRUAL to 5,
            Phase.FOLLICULAR to 9,
            Phase.OVULATION to 1,
            Phase.LUTEAL to 13,
        ).forEach { (p, weight) ->
            Box(
                modifier = Modifier
                    .weight(weight.toFloat())
                    .size(0.dp, 6.dp)
                    .background(
                        if (p == current) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}

private fun phaseLabel(p: Phase) = when (p) {
    Phase.MENSTRUAL -> "Menstrual"
    Phase.FOLLICULAR -> "Follicular"
    Phase.OVULATION -> "Ovulation"
    Phase.LUTEAL -> "Luteal"
}
