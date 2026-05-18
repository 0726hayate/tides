package com.hayate0726.tides.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    onRestore: (primaryPin: String, backupPassword: String, source: Uri) -> Unit,
    onDismissStatus: () -> Unit,
) {
    var exportPin by remember { mutableStateOf("") }
    var exportBackupPw by remember { mutableStateOf("") }

    var restorePin by remember { mutableStateOf("") }
    var restoreBackupPw by remember { mutableStateOf("") }
    var pickedUri by remember { mutableStateOf<Uri?>(null) }
    var confirmRestore by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> pickedUri = uri }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
    ) {
        Text("Backup & restore", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.size(8.dp))
        Text(
            "Backups are encrypted with a separate password and stay on your " +
                "device until you share them.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(24.dp))

        Text("Export", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(8.dp))
        PinField(value = exportPin, onChange = { exportPin = it }, label = "Primary PIN")
        Spacer(Modifier.size(8.dp))
        OutlinedTextField(
            value = exportBackupPw,
            onValueChange = { exportBackupPw = it },
            label = { Text("Backup password (you'll need this to restore)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(12.dp))

        if (status is BackupViewModel.Status.Ready) {
            Text("Backup ready: ${status.filename}",
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.size(8.dp))
            Button(onClick = { onShare(status.uri) }, modifier = Modifier.fillMaxWidth()) {
                Text("Share backup file")
            }
        } else {
            Button(
                onClick = { onExport(exportPin, exportBackupPw) },
                enabled = exportPin.length >= 6 && exportBackupPw.isNotEmpty()
                    && status !is BackupViewModel.Status.Working,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Encrypt and stage backup") }
        }

        Spacer(Modifier.size(24.dp))
        HorizontalDivider()
        Spacer(Modifier.size(24.dp))

        Text("Restore", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(4.dp))
        Text(
            "Restoring replaces your current cycle data. Your PIN is unchanged.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(12.dp))
        OutlinedButton(
            onClick = { filePicker.launch(arrayOf("*/*")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(pickedUri?.lastPathSegment ?: "Pick a backup file")
        }
        Spacer(Modifier.size(8.dp))
        PinField(value = restorePin, onChange = { restorePin = it }, label = "Primary PIN")
        Spacer(Modifier.size(8.dp))
        OutlinedTextField(
            value = restoreBackupPw,
            onValueChange = { restoreBackupPw = it },
            label = { Text("Backup password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(12.dp))
        Button(
            onClick = { confirmRestore = true },
            enabled = pickedUri != null
                && restorePin.length >= 6
                && restoreBackupPw.isNotEmpty()
                && status !is BackupViewModel.Status.Working,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Restore from backup") }

        Spacer(Modifier.size(16.dp))
        when (status) {
            BackupViewModel.Status.Working -> {
                Text("Working…", style = MaterialTheme.typography.bodySmall)
            }
            is BackupViewModel.Status.Error -> {
                Text(status.message, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.size(8.dp))
                OutlinedButton(onClick = onDismissStatus, modifier = Modifier.fillMaxWidth()) {
                    Text("Clear")
                }
            }
            BackupViewModel.Status.RestoreComplete -> {
                Text(
                    "Restore complete. The app will lock — unlock with your primary PIN " +
                        "to see the restored data.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            else -> {}
        }
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            title = { Text("Replace current data?") },
            text = {
                Text(
                    "This will permanently replace your current cycle data with the contents " +
                        "of the backup. Your primary PIN does not change. Continue?",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestore = false
                    pickedUri?.let { onRestore(restorePin, restoreBackupPw, it) }
                }) { Text("Replace") }
            },
            dismissButton = {
                TextButton(onClick = { confirmRestore = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun PinField(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all(Char::isDigit) && it.length <= 12) onChange(it) },
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
    )
}
