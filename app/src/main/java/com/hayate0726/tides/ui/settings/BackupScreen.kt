package com.hayate0726.tides.ui.settings

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun BackupScreen(
    status: BackupViewModel.Status,
    onExport: (primaryPin: String, backupPassword: String) -> Unit,
    onShare: (Uri) -> Unit,
    onImport: () -> Unit,
    onDismissStatus: () -> Unit,
) {
    var primaryPin by remember { mutableStateOf("") }
    var backupPw by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Backup & restore", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.size(8.dp))
        Text(
            "Backups are encrypted with a separate password and stay on your " +
                "device until you share them. Restoring is not yet supported in " +
                "this build.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(24.dp))

        Text("Export", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(8.dp))
        OutlinedTextField(
            value = primaryPin,
            onValueChange = { if (it.all(Char::isDigit) && it.length <= 12) primaryPin = it },
            label = { Text("Primary PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(8.dp))
        OutlinedTextField(
            value = backupPw,
            onValueChange = { backupPw = it },
            label = { Text("Backup password (you'll need this to restore)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(12.dp))

        when (status) {
            BackupViewModel.Status.Working -> {
                Text("Encrypting…", style = MaterialTheme.typography.bodySmall)
            }
            is BackupViewModel.Status.Ready -> {
                Text("Backup ready: ${status.filename}",
                    style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.size(8.dp))
                Button(onClick = { onShare(status.uri) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Share backup file")
                }
            }
            is BackupViewModel.Status.Error -> {
                Text(status.message, color = MaterialTheme.colorScheme.error)
            }
            BackupViewModel.Status.NotWired -> {
                Text(
                    "Restore is not yet supported in this build. " +
                        "Keep your backup file — it will be importable in a future update.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            BackupViewModel.Status.Idle -> {}
        }
        if (status !is BackupViewModel.Status.Ready) {
            Spacer(Modifier.size(12.dp))
            Button(
                onClick = { onExport(primaryPin, backupPw) },
                enabled = primaryPin.length >= 6 && backupPw.isNotEmpty()
                    && status !is BackupViewModel.Status.Working,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Encrypt and stage backup") }
        }

        Spacer(Modifier.size(32.dp))
        Text("Restore", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(8.dp))
        OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) {
            Text("Restore from backup (not yet supported)")
        }

        Spacer(Modifier.size(16.dp))
        if (status != BackupViewModel.Status.Idle) {
            OutlinedButton(onClick = onDismissStatus, modifier = Modifier.fillMaxWidth()) {
                Text("Clear")
            }
        }
    }
}
