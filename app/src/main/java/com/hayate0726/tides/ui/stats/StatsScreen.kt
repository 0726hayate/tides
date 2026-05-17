package com.hayate0726.tides.ui.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hayate0726.tides.domain.CycleStats
import com.hayate0726.tides.domain.model.Symptom

data class StatsUiState(
    val cycleStats: CycleStats,
    val cycleLengthsForChart: List<Int>,
    val cycleLengthLabels: List<String>,
    val symptomFrequency: Map<Symptom, Int>,
    val insight: String?,
)

@Composable
fun StatsScreen(
    state: StatsUiState,
    onDismissInsight: () -> Unit,
    onExportPdf: () -> Unit,  // wired in Plan 4
    onExportCsv: () -> Unit,  // wired in Plan 4
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("Insights", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.size(20.dp))

        if (state.insight != null) {
            InsightCard(text = state.insight, onDismiss = onDismissInsight)
            Spacer(Modifier.size(20.dp))
        }

        Row {
            SummaryCard("AVG CYCLE", state.cycleStats.medianCycleLength?.let { "$it" } ?: "—", "days",
                modifier = Modifier.weight(1f))
            Spacer(Modifier.size(10.dp))
            SummaryCard("AVG PERIOD", state.cycleStats.medianPeriodLength?.let { "$it" } ?: "—", "days",
                modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.size(20.dp))

        Text("CYCLE LENGTH", style = MaterialTheme.typography.labelMedium,
             color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.size(10.dp))
        CycleLengthChart(values = state.cycleLengthsForChart, labels = state.cycleLengthLabels)

        Spacer(Modifier.size(20.dp))
        Text("TOP SYMPTOMS", style = MaterialTheme.typography.labelMedium,
             color = MaterialTheme.colorScheme.onSurfaceVariant)
        SymptomFrequencyList(frequency = state.symptomFrequency)
    }
}

@Composable
private fun SummaryCard(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium,
                 color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.size(4.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.size(4.dp))
                Text(unit, style = MaterialTheme.typography.labelMedium,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
