package com.hayate0726.tides.ui.onboarding

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hayate0726.tides.domain.model.Goal

@Composable
fun GoalsScreen(
    initialGoals: Set<Goal>,
    onContinue: (Set<Goal>) -> Unit,
) {
    var selected by remember { mutableStateOf(initialGoals) }

    val items = listOf(
        Goal.TRACK_PERIOD to ("Track my period" to "Log days, see history"),
        Goal.TRACK_SYMPTOMS to ("Track symptoms" to "Cramps, mood, anything else"),
        Goal.MANAGE_CONDITION to ("Manage a condition" to "PCOS, endometriosis, perimenopause"),
        Goal.AVOID_PREGNANCY to ("Avoid pregnancy" to "Shows ovulation window — not for contraception"),
        Goal.TRYING_TO_CONCEIVE to ("Trying to conceive" to "Fertile-window focus"),
        Goal.JUST_CURIOUS to ("Just curious" to "Get to know my cycle"),
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
    ) {
        Text("What are you using Tides for?", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.size(8.dp))
        Text(
            "Pick what fits. We'll only show features that match. You can change this anytime.",
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
                        .clickable {
                            selected = if (checked) selected - goal else selected + goal
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = if (checked) MaterialTheme.colorScheme.background
                            else MaterialTheme.colorScheme.surface,
                    border = if (checked) androidx.compose.foundation.BorderStroke(
                        1.5.dp, MaterialTheme.colorScheme.onSurface
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
                            Text(desc, style = MaterialTheme.typography.bodySmall,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.size(24.dp))
        Button(onClick = { onContinue(selected) }, modifier = Modifier.fillMaxWidth()) {
            Text("Continue")
        }
    }
}
