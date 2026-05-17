package com.hayate0726.tides.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.hayate0726.tides.domain.model.Symptom

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SymptomPicker(
    selectedWithSeverity: Map<Symptom, Int>,
    onToggle: (Symptom) -> Unit,
    onSeverityChange: (Symptom, Int) -> Unit,
) {
    val real = listOf(
        Symptom.CRAMPS to "Cramps",
        Symptom.HEADACHE to "Headache",
        Symptom.BLOATING to "Bloating",
        Symptom.TIRED to "Fatigue",
        Symptom.BACK_PAIN to "Back pain",
        Symptom.NAUSEA to "Nausea",
        Symptom.BREAST_TENDERNESS to "Tender breasts",
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        real.forEach { (sym, label) ->
            val sev = selectedWithSeverity[sym]
            val isSelected = sev != null
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (isSelected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.surface,
                onClick = { onToggle(sym) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        label,
                        color = if (isSelected) MaterialTheme.colorScheme.background
                                else MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (sev != null) {
                        Spacer(Modifier.size(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.background.copy(alpha = 0.2f),
                            onClick = { onSeverityChange(sym, (sev + 1) % 3) },
                        ) {
                            Text(
                                listOf("mild", "moderate", "severe")[sev],
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.background,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
