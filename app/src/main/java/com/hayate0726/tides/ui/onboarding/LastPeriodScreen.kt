package com.hayate0726.tides.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LastPeriodScreen(onFinish: (LocalDate?) -> Unit) {
    val state = rememberDatePickerState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("When did your last period start?", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.size(8.dp))
        Text(
            "Optional — predictions get better as you log more.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(16.dp))
        DatePicker(state = state, showModeToggle = false)
        Spacer(Modifier.size(24.dp))
        Row {
            OnboardingSecondaryButton(
                text = "Skip",
                onClick = { onFinish(null) },
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.size(12.dp))
            OnboardingPrimaryButton(
                text = "Done",
                onClick = {
                    val date = state.selectedDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    onFinish(date)
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}
