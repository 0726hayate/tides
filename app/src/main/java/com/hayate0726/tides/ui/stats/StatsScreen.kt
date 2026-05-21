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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hayate0726.tides.domain.CycleStats
import com.hayate0726.tides.domain.model.Symptom

data class StatsUiState(
    val cycleStats: CycleStats,
    val cycleLengthsForChart: List<Int>,
    val cycleLengthLabels: List<String>,
    val periodLengthsForChart: List<Int>,
    val symptomFrequency: Map<Symptom, Int>,
    val symptomHeatmap: Map<Symptom, Map<Int, Int>>,
    val insight: String?,
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    state: StatsUiState,
    range: StatsViewModel.Range,
    onRangeChange: (StatsViewModel.Range) -> Unit,
    onDismissInsight: () -> Unit,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit,
) {
    var showDetails by remember { mutableStateOf(false) }
    var infoText by remember { mutableStateOf<String?>(null) }
    var infoSheet by remember { mutableStateOf<SheetCopy?>(null) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Text("Insights", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.size(12.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            StatsViewModel.Range.entries.forEachIndexed { idx, r ->
                SegmentedButton(
                    selected = r == range,
                    onClick = { onRangeChange(r) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = idx, count = StatsViewModel.Range.entries.size,
                    ),
                ) { Text(rangeLabel(r)) }
            }
        }
        Spacer(Modifier.size(20.dp))

        if (state.insight != null) {
            InsightCard(
                text = state.insight,
                onDismiss = onDismissInsight,
                onInfoClick = { infoSheet = StatsInfoCopy.insightCard },
            )
            Spacer(Modifier.size(20.dp))
        }

        Row {
            SummaryCard(
                label = "AVG CYCLE",
                value = state.cycleStats.medianCycleLength?.let { "$it" } ?: "—",
                unit = "days",
                onInfo = { infoText = INFO_AVG_CYCLE },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.size(10.dp))
            SummaryCard(
                label = "AVG PERIOD",
                value = state.cycleStats.medianPeriodLength?.let { "$it" } ?: "—",
                unit = "days",
                onInfo = { infoText = INFO_AVG_PERIOD },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.size(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "CYCLE LENGTH",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { infoText = INFO_REGULARITY }) {
                Text("What does this mean?", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = { infoSheet = StatsInfoCopy.cycleLength }) {
                Icon(Icons.Outlined.Info, contentDescription = "About cycle length")
            }
        }
        Spacer(Modifier.size(10.dp))
        CycleLengthChart(values = state.cycleLengthsForChart, labels = state.cycleLengthLabels)

        Spacer(Modifier.size(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "TOP SYMPTOMS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { infoSheet = StatsInfoCopy.symptomFrequency }) {
                Icon(Icons.Outlined.Info, contentDescription = "About symptom frequency")
            }
        }
        SymptomFrequencyList(frequency = state.symptomFrequency)

        Spacer(Modifier.size(24.dp))
        TextButton(onClick = { showDetails = !showDetails }) {
            Text(if (showDetails) "Hide details" else "Show details")
        }
        if (showDetails) {
            Spacer(Modifier.size(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "PERIOD LENGTH",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { infoSheet = StatsInfoCopy.periodLength }) {
                    Icon(Icons.Outlined.Info, contentDescription = "About period length")
                }
            }
            Spacer(Modifier.size(6.dp))
            PeriodLengthTrend(values = state.periodLengthsForChart)
            Spacer(Modifier.size(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "SYMPTOM HEATMAP",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { infoSheet = StatsInfoCopy.symptomHeatmap }) {
                    Icon(Icons.Outlined.Info, contentDescription = "About symptom heatmap")
                }
            }
            Spacer(Modifier.size(6.dp))
            SymptomHeatmap(heatmap = state.symptomHeatmap)
        }

        Spacer(Modifier.size(32.dp))
        Row {
            OutlinedButton(
                onClick = onExportPdf,
                enabled = state.cycleStats.completedCycleCount > 0,
                modifier = Modifier.weight(1f),
            ) { Text("Share PDF") }
            Spacer(Modifier.size(12.dp))
            OutlinedButton(
                onClick = onExportCsv,
                enabled = state.cycleStats.completedCycleCount > 0,
                modifier = Modifier.weight(1f),
            ) { Text("Share CSV") }
        }
    }

    val it = infoText
    if (it != null) {
        AlertDialog(
            onDismissRequest = { infoText = null },
            confirmButton = { TextButton(onClick = { infoText = null }) { Text("OK") } },
            text = { Text(it) },
        )
    }

    infoSheet?.let { copy ->
        StatsInfoSheet(copy = copy, onDismiss = { infoSheet = null })
    }
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    unit: String,
    onInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onInfo) { Text("?", style = MaterialTheme.typography.labelMedium) }
            }
            Spacer(Modifier.size(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = MaterialTheme.typography.displayMedium)
                Spacer(Modifier.size(4.dp))
                Text(unit, style = MaterialTheme.typography.labelMedium,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun rangeLabel(r: StatsViewModel.Range) = when (r) {
    StatsViewModel.Range.THREE_MO -> "3mo"
    StatsViewModel.Range.SIX_MO -> "6mo"
    StatsViewModel.Range.ONE_YR -> "1y"
    StatsViewModel.Range.ALL -> "All"
}

private const val INFO_AVG_CYCLE =
    "Median number of days from one period start to the next, across the selected range. " +
        "A typical cycle is 24–38 days (FIGO)."

private const val INFO_AVG_PERIOD =
    "Median number of bleeding days per cycle, across the selected range. " +
        "FIGO flags >8 days as prolonged."

private const val INFO_REGULARITY =
    "FIGO defines a cycle as irregular if the shortest-to-longest variation across cycles " +
        "is more than 7 days."
