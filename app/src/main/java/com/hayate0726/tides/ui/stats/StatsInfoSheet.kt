package com.hayate0726.tides.ui.stats

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsInfoSheet(
    copy: SheetCopy,
    onDismiss: () -> Unit,
) {
    val state = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(copy.title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.size(12.dp))
            Text(copy.body, style = MaterialTheme.typography.bodyMedium)
            if (copy.sources.isNotEmpty()) {
                Spacer(Modifier.size(16.dp))
                Text(
                    "Sources: " + copy.sources.joinToString("; "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.size(24.dp))
            Text(
                "All charts are generated on your device. Nothing is uploaded.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(24.dp))
        }
    }
}
