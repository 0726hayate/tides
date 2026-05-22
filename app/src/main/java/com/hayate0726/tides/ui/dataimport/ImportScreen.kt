package com.hayate0726.tides.ui.dataimport

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ImportScreen(
    vm: ImportViewModel,
    onPick: (Uri) -> Unit,
) {
    val status by vm.status.collectAsStateWithLifecycle()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) onPick(uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Import from another app", style = MaterialTheme.typography.headlineLarge)
        Text(
            "We can read exports from:\n  • Samsung Health (HTML report)\n  • Clue (CSV)\n  • drip (JSON)\n\nPick the export file you saved to your phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(8.dp))
        Button(
            onClick = {
                launcher.launch(arrayOf("text/html", "text/csv", "application/json", "*/*"))
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = status !is ImportViewModel.Status.Parsing,
        ) {
            Text("Pick file")
        }

        when (val s = status) {
            ImportViewModel.Status.Parsing -> {
                Spacer(Modifier.size(8.dp))
                CircularProgressIndicator()
                Text("Reading file…", style = MaterialTheme.typography.bodyMedium)
            }
            is ImportViewModel.Status.Error -> {
                Text(
                    s.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            else -> Unit
        }
    }
}
