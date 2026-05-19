package com.hayate0726.tides.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text("Tides", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.size(8.dp))
        Text(
            "Your data never leaves this phone. There is no account. " +
                "No one — including the developer — can see what you log.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.size(32.dp))
        Text("Before we start", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(4.dp))
        Text(
            "A few things to know about Tides:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        DisclaimerItem(
            title = "Your data stays on your device.",
            body = "Tides has no internet permission. There is no cloud, no " +
                "account, no server. Your cycle data never leaves your phone.",
        )
        DisclaimerItem(
            title = "Tides is not medical advice.",
            body = "Patterns and predictions shown by Tides are estimates from " +
                "your own logged data, not diagnoses. For health questions, " +
                "please talk to a qualified healthcare professional.",
        )
        DisclaimerItem(
            title = "Tides is not a contraceptive.",
            body = "Cycle predictions are not reliable for preventing pregnancy. " +
                "If you are trying to avoid pregnancy, use a method that is " +
                "actually designed for that purpose.",
        )
        DisclaimerItem(
            title = "Tides is a personal project.",
            body = "I built this for my girlfriend and I'm sharing it freely. " +
                "It's provided as-is under GPL-3.0, with no warranty. By " +
                "continuing, you understand it's a gift to the community, not " +
                "a professional service.",
        )

        Spacer(Modifier.size(32.dp))
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text("I understand — set up Tides")
        }
        Spacer(Modifier.size(24.dp))
    }
}

@Composable
private fun DisclaimerItem(title: String, body: String) {
    Spacer(Modifier.size(20.dp))
    Text(title, style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.size(4.dp))
    Text(
        body,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
