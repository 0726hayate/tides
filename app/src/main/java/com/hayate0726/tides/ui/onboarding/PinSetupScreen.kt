package com.hayate0726.tides.ui.onboarding

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
fun PinSetupScreen(onContinue: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    val error = remember(pin, confirm) {
        when {
            pin.isEmpty() -> null
            pin.length < 6 -> "PIN must be at least 6 digits"
            pin != confirm && confirm.isNotEmpty() -> "PINs don't match"
            else -> null
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Set a PIN", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.size(8.dp))
        Text(
            "Used to encrypt your data. If you forget it, your data is unrecoverable — there is no reset.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(24.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 12 && it.all { c -> c.isDigit() }) pin = it },
            label = { Text("PIN (6+ digits)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.size(12.dp))
        OutlinedTextField(
            value = confirm,
            onValueChange = { if (it.length <= 12 && it.all { c -> c.isDigit() }) confirm = it },
            label = { Text("Confirm PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
        )

        if (error != null) {
            Spacer(Modifier.size(8.dp))
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.size(24.dp))
        Button(
            onClick = { onContinue(pin) },
            enabled = pin.length >= 6 && pin == confirm,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Continue") }
    }
}
