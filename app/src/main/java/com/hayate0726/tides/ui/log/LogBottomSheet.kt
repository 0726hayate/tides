package com.hayate0726.tides.ui.log

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun LogBottomSheet(
    date: LocalDate,
    cycleDay: Int?,
    initialFlow: FlowIntensity?,
    initialSymptoms: Map<Symptom, Int>,
    initialNote: String,
    onSave: (FlowIntensity?, Map<Symptom, Int>, String) -> Unit,
    onCancel: () -> Unit,
) {
    var flow by remember { mutableStateOf(initialFlow) }
    var symptoms by remember { mutableStateOf(initialSymptoms) }
    var note by remember { mutableStateOf(initialNote) }

    Column(Modifier.fillMaxWidth().padding(24.dp)) {
        Row {
            Text(
                date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            if (cycleDay != null) {
                Text(
                    "Cycle day $cycleDay",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.size(20.dp))
        Text(
            "FLOW",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(10.dp))
        FlowPicker(selected = flow, onSelect = { flow = it })

        Spacer(Modifier.size(20.dp))
        Text(
            "SYMPTOMS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(10.dp))
        SymptomPicker(
            selectedWithSeverity = symptoms,
            onToggle = { sym ->
                symptoms = if (sym in symptoms) symptoms - sym else symptoms + (sym to 0)
            },
            onSeverityChange = { sym, sev -> symptoms = symptoms + (sym to sev) },
        )

        Spacer(Modifier.size(20.dp))
        Text(
            "NOTE",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(6.dp))
        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            placeholder = { Text("Optional — anything else worth remembering") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
        )

        Spacer(Modifier.size(20.dp))
        Row {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Spacer(Modifier.size(12.dp))
            Button(
                onClick = { onSave(flow, symptoms, note) },
                modifier = Modifier.weight(1f),
            ) { Text("Save") }
        }
    }
}
