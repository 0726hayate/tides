package com.hayate0726.tides.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NotificationsScreen(
    periodPredictedEnabled: Boolean,
    periodStartEnabled: Boolean,
    latePeriodEnabled: Boolean,
    systemNotificationsEnabled: Boolean,
    onTogglePredicted: (Boolean) -> Unit,
    onToggleStart: (Boolean) -> Unit,
    onToggleLate: (Boolean) -> Unit,
    onOpenSystemSettings: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text(
            "All reminders are off by default and run only on this device. " +
                "Tides has no internet permission.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(24.dp))

        if (!systemNotificationsEnabled) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSystemSettings() }
                    .padding(vertical = 12.dp),
            ) {
                Text(
                    "Notifications are blocked for Tides",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    "Tap to enable in system settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
            Spacer(Modifier.size(8.dp))
        }

        ToggleRow(
            label = "Period predicted",
            description = "3 days before your predicted period.",
            checked = periodPredictedEnabled,
            enabled = systemNotificationsEnabled,
            onCheckedChange = onTogglePredicted,
        )
        ToggleRow(
            label = "Period start",
            description = "On your predicted period day.",
            checked = periodStartEnabled,
            enabled = systemNotificationsEnabled,
            onCheckedChange = onToggleStart,
        )
        ToggleRow(
            label = "Late period",
            description = "If your period is 3 days later than predicted.",
            checked = latePeriodEnabled,
            enabled = systemNotificationsEnabled,
            onCheckedChange = onToggleLate,
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
    HorizontalDivider()
}
