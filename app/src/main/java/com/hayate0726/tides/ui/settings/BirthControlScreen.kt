package com.hayate0726.tides.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hayate0726.tides.domain.model.BirthControlMethod

@Composable
fun BirthControlScreen(
    state: BirthControlViewModel.UiState,
    onSelect: (BirthControlMethod) -> Unit,
    onSave: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            "Used only to tailor the fertile-window display. Tides never sends this data anywhere.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(20.dp))

        BirthControlMethod.entries.forEach { m ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { onSelect(m) }
                    .padding(vertical = 10.dp),
            ) {
                RadioButton(selected = state.selected == m, onClick = { onSelect(m) })
                Spacer(Modifier.size(8.dp))
                Text(label(m), style = MaterialTheme.typography.bodyLarge,
                     modifier = Modifier.weight(1f))
                if (m.isHormonal) {
                    Text(
                        "hormonal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.size(20.dp))
        Button(
            onClick = onSave,
            enabled = state.selected != state.current,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (state.saved) "Saved" else "Save") }
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
    BirthControlMethod.OTHER -> "Other"
}
