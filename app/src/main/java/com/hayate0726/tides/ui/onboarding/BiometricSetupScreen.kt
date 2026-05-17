package com.hayate0726.tides.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BiometricSetupScreen(
    initialEnabled: Boolean,
    onContinue: (Boolean) -> Unit,
) {
    var enabled by remember { mutableStateOf(initialEnabled) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Faster unlock?", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.size(8.dp))
        Text(
            "Use your fingerprint or face to unlock faster. Your PIN is still required if biometric fails.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Enable biometric unlock", modifier = Modifier.weight(1f))
            Switch(checked = enabled, onCheckedChange = { enabled = it })
        }

        Spacer(Modifier.size(24.dp))
        Button(onClick = { onContinue(enabled) }, modifier = Modifier.fillMaxWidth()) {
            Text("Continue")
        }
    }
}
