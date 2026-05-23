package com.hayate0726.tides.ui.dataimport

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hayate0726.tides.data.`import`.ImportResult

@Composable
fun ImportResultScreen(
    result: ImportResult,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text("✓ Import complete", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.size(12.dp))
        Text("Added ${result.addedDates} new dates.", style = MaterialTheme.typography.bodyLarge)
        if (result.skippedDates > 0) {
            Spacer(Modifier.size(4.dp))
            Text("Kept ${result.skippedDates} existing entries as-is.", style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.size(16.dp))
        Text(
            "You may want to delete the export file from your phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(32.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}
