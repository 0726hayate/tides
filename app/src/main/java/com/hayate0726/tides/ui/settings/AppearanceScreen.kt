package com.hayate0726.tides.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

@Composable
fun AppearanceScreen(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    useDynamicColor: Boolean,
    onToggleDynamicColor: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Theme", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(4.dp))
        Text(
            "Choose light, dark, or follow your device's setting.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(12.dp))

        ThemeModeRow("Follow system", ThemeMode.SYSTEM, themeMode, onThemeModeChange)
        ThemeModeRow("Light", ThemeMode.LIGHT, themeMode, onThemeModeChange)
        ThemeModeRow("Dark", ThemeMode.DARK, themeMode, onThemeModeChange)

        Spacer(Modifier.size(24.dp))
        HorizontalDivider()
        Spacer(Modifier.size(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = useDynamicColor,
                    role = Role.Switch,
                    onValueChange = onToggleDynamicColor,
                )
                .padding(vertical = 12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Match system colors", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Use Material You dynamic colors. Cycle marks use shape " +
                        "(drops, diamonds, dashes) so they remain readable on any palette.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = useDynamicColor, onCheckedChange = null)
        }
        Spacer(Modifier.size(8.dp))
        Text(
            "Tides ships with a color-blind-safe palette by default.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ThemeModeRow(
    label: String,
    mode: ThemeMode,
    current: ThemeMode,
    onChange: (ThemeMode) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = mode == current,
                role = Role.RadioButton,
                onClick = { onChange(mode) },
            )
            .padding(vertical = 8.dp),
    ) {
        RadioButton(selected = mode == current, onClick = null)
        Spacer(Modifier.size(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
