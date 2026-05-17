package com.hayate0726.tides.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Transient screen shown after the user finishes the LastPeriod step while
 * [OnboardingViewModel.complete] is running on a background dispatcher
 * (Argon2 derivation + SQLCipher open take a couple of seconds on first
 * launch). The nav graph observes `OnboardingViewModel.completion` and
 * transitions to the main route as soon as the database handle is emitted;
 * this composable just gives the user something to look at in the
 * meantime.
 */
@Composable
fun OnboardingComplete() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.size(16.dp))
        Text(
            "Setting up your private space…",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
