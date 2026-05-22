package com.hayate0726.tides.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ImportPromptScreen(
    onImport: () -> Unit,
    onStartFresh: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Bring your data over?", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.size(8.dp))
        Text(
            "If you've been using another period tracker, you can import your history " +
                "now — Samsung Health, Clue, or drip. Nothing gets uploaded; the file " +
                "stays on your phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.weight(1f))

        OnboardingPrimaryButton(
            text = "Import from another app",
            onClick = onImport,
        )
        Spacer(Modifier.size(12.dp))
        OnboardingSecondaryButton(
            text = "Start fresh",
            onClick = onStartFresh,
        )
    }
}
