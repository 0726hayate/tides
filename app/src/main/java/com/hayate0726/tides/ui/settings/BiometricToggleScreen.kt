package com.hayate0726.tides.ui.settings

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
fun BiometricToggleScreen(
    enrolled: Boolean,
    status: BiometricToggleViewModel.Status,
    onEnable: (primaryPin: String) -> Unit,
    onDisable: () -> Unit,
    onDismissStatus: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            "Use your fingerprint or face to unlock Tides without typing your PIN. " +
                "Your PIN is still required if biometric fails or after re-enrolling fingerprints.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(24.dp))

        if (enrolled) {
            Text("Biometric unlock is enabled.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.size(12.dp))
            OutlinedButton(onClick = onDisable, modifier = Modifier.fillMaxWidth()) {
                Text("Disable biometric unlock")
            }
        } else {
            Text("Enter your primary PIN to enable.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.size(8.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.all(Char::isDigit) && it.length <= 12) pin = it },
                label = { Text("Primary PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.size(12.dp))
            Button(
                onClick = { onEnable(pin) },
                enabled = pin.length >= 6 && status !is BiometricToggleViewModel.Status.Working
                    && status !is BiometricToggleViewModel.Status.AwaitingBiometric,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Enable biometric unlock") }
        }

        Spacer(Modifier.size(16.dp))
        when (status) {
            BiometricToggleViewModel.Status.Working -> Text("Working…")
            BiometricToggleViewModel.Status.AwaitingBiometric -> Text("Waiting for biometric…")
            BiometricToggleViewModel.Status.EnrolledOk -> Text("Biometric unlock enabled.")
            BiometricToggleViewModel.Status.DisabledOk -> Text("Biometric unlock disabled.")
            is BiometricToggleViewModel.Status.Error -> Text(
                status.message, color = MaterialTheme.colorScheme.error,
            )
            BiometricToggleViewModel.Status.Idle -> {}
        }
        if (status != BiometricToggleViewModel.Status.Idle &&
            status != BiometricToggleViewModel.Status.Working &&
            status != BiometricToggleViewModel.Status.AwaitingBiometric) {
            Spacer(Modifier.size(8.dp))
            OutlinedButton(onClick = onDismissStatus, modifier = Modifier.fillMaxWidth()) {
                Text("Clear")
            }
        }
    }
}
