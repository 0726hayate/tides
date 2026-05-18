package com.hayate0726.tides.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/**
 * Backup & restore screen. Wiring to BackupExporter/BackupImporter and the
 * Storage Access Framework is handled by the host (ViewModel + Activity),
 * which receives the chosen password and the user-picked Uri.
 *
 * v1 keeps the UI thin: two password fields and two buttons. The host is
 * expected to confirm overwrite, surface progress, and report errors.
 */
@Composable
fun BackupScreen(
    onExport: (backupPassword: String) -> Unit,
    onImport: (backupPassword: String) -> Unit,
) {
    var exportPw by remember { mutableStateOf("") }
    var importPw by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Backup & restore", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.size(24.dp))

        Text("Export", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(8.dp))
        Text(
            "Choose a backup password. You will need it to restore. " +
                "The password is separate from your unlock PIN.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(8.dp))
        OutlinedTextField(
            value = exportPw,
            onValueChange = { exportPw = it },
            label = { Text("Backup password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(8.dp))
        Button(
            onClick = { onExport(exportPw) },
            enabled = exportPw.isNotEmpty(),
        ) {
            Text("Export backup")
        }

        Spacer(Modifier.size(32.dp))

        Text("Restore", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(8.dp))
        Text(
            "Restoring will replace your current data with the contents of the backup.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(8.dp))
        OutlinedTextField(
            value = importPw,
            onValueChange = { importPw = it },
            label = { Text("Backup password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(8.dp))
        Button(
            onClick = { onImport(importPw) },
            enabled = importPw.isNotEmpty(),
        ) {
            Text("Restore from backup")
        }
    }
}
