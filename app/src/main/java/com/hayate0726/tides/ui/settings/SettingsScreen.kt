package com.hayate0726.tides.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    onNotifications: () -> Unit,
    onBackup: () -> Unit,
    onDuress: () -> Unit,
    duressAvailable: Boolean,
    onCheckUpdates: () -> Unit,
    onSendFeedback: () -> Unit,
    onSupportDevelopment: () -> Unit,
    onLock: () -> Unit,
    appStateIsUnlocked: Boolean,
    onBiometric: () -> Unit,
    onBirthControl: () -> Unit,
    onAppearance: () -> Unit,
    onImport: () -> Unit,
    onRollback: () -> Unit,
    showRollback: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.size(24.dp))

        SettingsSectionHeader("Privacy")
        SettingsRow("Privacy preset", value = threatPresetLabel, onClick = onChangePreset)
        if (duressAvailable) {
            SettingsRow("Duress PIN", onClick = onDuress)
        }
        SettingsRow("Biometric unlock", onClick = onBiometric)
        SettingsRow("Birth control", onClick = onBirthControl)
        if (appStateIsUnlocked) {
            SettingsRow("Lock now", onClick = onLock)
        }

        Spacer(Modifier.size(20.dp))
        SettingsSectionHeader("Data")
        SettingsRow("Reminders", onClick = onNotifications)
        SettingsRow("Backup & restore", onClick = onBackup)
        SettingsRow("Import from another app", onClick = onImport)
        if (showRollback) {
            SettingsRow("Roll back last import", onClick = onRollback)
        }

        Spacer(Modifier.size(20.dp))
        SettingsSectionHeader("About")
        SettingsRow("Appearance", onClick = onAppearance)
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
            Text(
                value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surface)
}
