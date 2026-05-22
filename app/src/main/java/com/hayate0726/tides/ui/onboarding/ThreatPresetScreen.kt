package com.hayate0726.tides.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hayate0726.tides.domain.model.ThreatPreset

@Composable
fun ThreatPresetScreen(
    initial: ThreatPreset,
    onContinue: (ThreatPreset) -> Unit,
) {
    var selected by rememberSaveable(
        stateSaver = Saver<ThreatPreset, String>(
            save = { it.name },
            restore = { ThreatPreset.valueOf(it) },
        ),
    ) { mutableStateOf(initial) }

    val options = listOf(
        Triple(ThreatPreset.JUST_FOR_ME, "Just for me",
            "No lock screen. Anyone with your phone can open the app."),
        Triple(ThreatPreset.LOCKED_WHEN_AWAY, "★ Locked when away",
            "Recommended. PIN required after 5 minutes of background. Quick to unlock."),
        Triple(ThreatPreset.ALWAYS_LOCKED, "Always locked",
            "Maximum privacy. PIN every 30 seconds. Optional panic features."),
    )

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("How private does this need to be?", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.size(8.dp))
        Text(
            "You can change this anytime in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { (preset, label, desc) ->
                val checked = preset == selected
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { selected = preset },
                    shape = RoundedCornerShape(16.dp),
                    color = if (checked) MaterialTheme.colorScheme.background
                            else MaterialTheme.colorScheme.surface,
                    border = if (checked) androidx.compose.foundation.BorderStroke(
                        1.5.dp, MaterialTheme.colorScheme.onSurface
                    ) else null,
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(label, style = MaterialTheme.typography.titleMedium)
                        Text(desc, style = MaterialTheme.typography.bodySmall,
                             color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(Modifier.size(24.dp))
        OnboardingPrimaryButton(
            text = "Continue",
            onClick = { onContinue(selected) },
        )
    }
}
