package com.hayate0726.tides.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hayate0726.tides.domain.model.Goal

@Composable
fun GoalsEditorScreen(
    selected: Set<Goal>,
    onToggle: (Goal) -> Unit,
    onSave: () -> Unit,
) {
    val items = listOf(
        Goal.TRACK_PERIOD to ("Track my period" to "Log days, see history"),
        Goal.TRACK_SYMPTOMS to ("Track symptoms" to "Cramps, mood, anything else"),
        Goal.MANAGE_CONDITION to ("Manage a condition" to "PCOS, endometriosis, perimenopause"),
        Goal.AVOID_PREGNANCY to ("Avoid pregnancy" to "Shows ovulation window — not for contraception"),
        Goal.TRYING_TO_CONCEIVE to ("Trying to conceive" to "Fertile-window focus"),
        Goal.JUST_CURIOUS to ("Just curious" to "Get to know my cycle"),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text("Goals", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.size(8.dp))
        Text(
            "Tides shows features that match your goals. Predictions and the " +
                "fertile-window view appear by default as long as you aren't on " +
                "hormonal birth control.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(24.dp))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items.forEach { (goal, copy) ->
                val (label, desc) = copy
                val checked = goal in selected
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(goal) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (checked) MaterialTheme.colorScheme.background
                            else MaterialTheme.colorScheme.surface,
                    border = if (checked) BorderStroke(
                        1.5.dp, MaterialTheme.colorScheme.onSurface,
                    ) else null,
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(
                                    if (checked) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.background,
                                    RoundedCornerShape(6.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (checked) Text("✓", color = MaterialTheme.colorScheme.onPrimary)
                        }
                        Spacer(Modifier.size(14.dp))
                        Column {
                            Text(label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                desc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.size(24.dp))
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = selected.isNotEmpty(),
        ) { Text("Save") }
    }
}
