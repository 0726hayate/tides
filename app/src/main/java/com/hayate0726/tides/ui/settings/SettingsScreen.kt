package com.hayate0726.tides.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    threatPresetLabel: String,
    onChangePreset: () -> Unit,
    onCheckUpdates: () -> Unit,
    onSendFeedback: () -> Unit,
    onSupportDevelopment: () -> Unit,
    onLock: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.size(24.dp))

        SettingsSectionHeader("Privacy")
        SettingsRow("Privacy preset", value = threatPresetLabel, onClick = onChangePreset)
        SettingsRow("Lock now", onClick = onLock)

        Spacer(Modifier.size(20.dp))
        SettingsSectionHeader("About")
        SettingsRow("Check for updates", onClick = onCheckUpdates)
        SettingsRow("Send feedback", onClick = onSendFeedback)
        SettingsRow("Support development (Ko-fi)", onClick = onSupportDevelopment)
    }
}

@Composable
private fun SettingsSectionHeader(label: String) {
    Text(
        label.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.size(8.dp))
}

@Composable
private fun SettingsRow(label: String, value: String? = null, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 14.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        if (value != null) {
            Text(value, style = MaterialTheme.typography.bodySmall,
                 color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surface)
}
