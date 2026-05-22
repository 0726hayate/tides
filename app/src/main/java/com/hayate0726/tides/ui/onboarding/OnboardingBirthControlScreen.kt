package com.hayate0726.tides.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hayate0726.tides.domain.model.BirthControlMethod

@Composable
fun OnboardingBirthControlScreen(
    initial: BirthControlMethod,
    onContinue: (BirthControlMethod) -> Unit,
) {
    var selected by rememberSaveable(
        stateSaver = Saver<BirthControlMethod, String>(
            save = { it.name },
            restore = { BirthControlMethod.valueOf(it) },
        ),
    ) { mutableStateOf(initial) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text("Birth control?", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.size(8.dp))
        Text(
            "Used only to tailor the fertile-window display. Hormonal methods " +
                "suppress ovulation predictions. You can change this anytime in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BirthControlMethod.entries.forEach { m ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selected = m }
                        .padding(vertical = 8.dp),
                ) {
                    RadioButton(
                        selected = selected == m,
                        onClick = { selected = m },
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        label(m),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (m.isHormonal) {
                        Text(
                            "hormonal",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.size(24.dp))
        OnboardingPrimaryButton(
            text = "Continue",
            onClick = { onContinue(selected) },
        )
        Spacer(Modifier.size(24.dp))
    }
}

private fun label(m: BirthControlMethod) = when (m) {
    BirthControlMethod.NONE -> "None"
    BirthControlMethod.PILL -> "Pill"
    BirthControlMethod.HORMONAL_IUD -> "Hormonal IUD"
    BirthControlMethod.COPPER_IUD -> "Copper IUD"
    BirthControlMethod.IMPLANT -> "Implant"
    BirthControlMethod.PATCH -> "Patch"
    BirthControlMethod.RING -> "Ring"
    BirthControlMethod.CONDOM -> "Condom"
    BirthControlMethod.OTHER -> "Other"
}
