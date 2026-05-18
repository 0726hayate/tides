package com.hayate0726.tides.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun DuressSetupScreen(
    onSave: (duressPin: String, mode: DuressMode) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(DuressMode.DECOY) }

    val valid = pin.length >= 6 && pin == confirm

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            "Optional. A second PIN that opens a fake or empty app — for when you can't refuse to unlock.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(24.dp))

        Text("Pick what happens when the duress PIN is entered:",
             style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.size(12.dp))
        DuressOption(DuressMode.DECOY, "Decoy data",
            "Opens a fake app with a small amount of plausibly-old data.",
            selected = mode == DuressMode.DECOY, onClick = { mode = DuressMode.DECOY })
        DuressOption(DuressMode.WIPE, "Panic wipe",
            "Deletes all data and resets the app. Irreversible.",
            selected = mode == DuressMode.WIPE, onClick = { mode = DuressMode.WIPE })

        Spacer(Modifier.size(24.dp))
        OutlinedTextField(
            value = pin, onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 12) pin = it },
            label = { Text("Duress PIN (6+ digits)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(12.dp))
        OutlinedTextField(
            value = confirm, onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 12) confirm = it },
            label = { Text("Confirm") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(24.dp))
        Button(
            onClick = { onSave(pin, mode) },
            enabled = valid,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save duress PIN") }
    }
}

enum class DuressMode { DECOY, WIPE }

@Composable
private fun DuressOption(
    mode: DuressMode,
    title: String,
    desc: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = androidx.compose.ui.Alignment.Top,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.size(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(desc, style = MaterialTheme.typography.bodySmall,
                 color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
