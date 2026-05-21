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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hayate0726.tides.domain.model.FlowIntensity
import com.hayate0726.tides.domain.model.Symptom

private val ONBOARDING_SYMPTOM_CHIPS: List<Symptom> = listOf(
    Symptom.CRAMPS,
    Symptom.HEADACHE,
    Symptom.BREAST_TENDERNESS,
    Symptom.BLOATING,
    Symptom.ACNE,
    Symptom.IRRITABLE,
    Symptom.SAD,
    Symptom.TIRED,
    Symptom.CRAVINGS,
)

// Labels chosen to match SymptomPicker (the full log sheet) so a user
// who sees "Fatigue" in onboarding sees "Fatigue" later, not "Tired".
private fun Symptom.label(): String = when (this) {
    Symptom.CRAMPS -> "Cramps"
    Symptom.HEADACHE -> "Headache"
    Symptom.BREAST_TENDERNESS -> "Tender breasts"
    Symptom.BLOATING -> "Bloating"
    Symptom.ACNE -> "Acne"
    Symptom.IRRITABLE -> "Irritable"
    Symptom.SAD -> "Sad"
    Symptom.TIRED -> "Fatigue"
    Symptom.CRAVINGS -> "Cravings"
    else -> name.lowercase().replaceFirstChar { it.uppercase() }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FlowSymptomsScreen(
    onSave: (FlowIntensity, Set<Symptom>) -> Unit,
    onSkipRest: (FlowIntensity, Set<Symptom>) -> Unit,
) {
    var flow by remember { mutableStateOf<FlowIntensity?>(null) }
    var selected by remember { mutableStateOf<Set<Symptom>>(emptySet()) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("How was that period?", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.size(8.dp))
        Text(
            "Helps your first predictions and chart.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(24.dp))

        Text("Flow", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val flowOptions = listOf(FlowIntensity.LIGHT, FlowIntensity.MEDIUM, FlowIntensity.HEAVY)
            flowOptions.forEachIndexed { idx, f ->
                SegmentedButton(
                    selected = flow == f,
                    onClick = { flow = f },
                    shape = SegmentedButtonDefaults.itemShape(idx, flowOptions.size),
                ) {
                    Text(when (f) {
                        FlowIntensity.LIGHT -> "Light"
                        FlowIntensity.MEDIUM -> "Medium"
                        FlowIntensity.HEAVY -> "Heavy"
                        else -> f.name
                    })
                }
            }
        }

        Spacer(Modifier.size(24.dp))
        Text("Anything else? (optional)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(8.dp))

        val rows = ONBOARDING_SYMPTOM_CHIPS.chunked(3)
        for (rowChips in rows) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                for (sym in rowChips) {
                    FilterChip(
                        selected = sym in selected,
                        onClick = {
                            selected = if (sym in selected) selected - sym else selected + sym
                        },
                        label = { Text(sym.label()) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - rowChips.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { flow?.let { onSkipRest(it, emptySet()) } },
                enabled = flow != null,
                modifier = Modifier.weight(1f),
            ) { Text("Skip the rest") }
            Button(
                onClick = { flow?.let { onSave(it, selected) } },
                enabled = flow != null,
                modifier = Modifier.weight(1f),
            ) { Text("Save") }
        }
    }
}
