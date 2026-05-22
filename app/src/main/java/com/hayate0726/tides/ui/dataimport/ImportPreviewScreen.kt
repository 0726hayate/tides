package com.hayate0726.tides.ui.dataimport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hayate0726.tides.data.`import`.PreviewState

@Composable
fun ImportPreviewScreen(
    preview: PreviewState,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Found ${preview.sourceName} export", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.size(8.dp))
        Text("${preview.totalEntries} period dates", style = MaterialTheme.typography.bodyLarge)

        if (preview.conflictingDates > 0) {
            Spacer(Modifier.size(8.dp))
            Text(
                "⚠ ${preview.conflictingDates} dates already in Tides will be kept as-is.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (preview.unmapped.isNotEmpty()) {
            Spacer(Modifier.size(8.dp))
            Text("⚠ Couldn't import:", style = MaterialTheme.typography.bodyMedium)
            for (u in preview.unmapped.take(5)) {
                Text(
                    "    ${u.sourceName} (${u.sampleCount})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (preview.unmapped.size > 5) {
                Text(
                    "    …and ${preview.unmapped.size - 5} more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (preview.warnings.isNotEmpty()) {
            Spacer(Modifier.size(8.dp))
            for (w in preview.warnings.take(3)) {
                Text(
                    "• $w",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.size(16.dp))
        Text(
            "Tides will save a snapshot before importing. You can roll it back from Settings.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.size(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(onClick = onConfirm, modifier = Modifier.weight(1f)) {
                Text("Import ${preview.totalEntries - preview.conflictingDates} dates")
            }
        }
    }
}
