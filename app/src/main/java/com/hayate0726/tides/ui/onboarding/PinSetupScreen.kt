package com.hayate0726.tides.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hayate0726.tides.ui.lock.PinKeypad
import kotlinx.coroutines.delay

private const val MIN_PIN_LENGTH = 6
private const val MAX_PIN_LENGTH = 12

@Composable
fun PinSetupScreen(onContinue: (String) -> Unit) {
    var firstPin by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf("") }
    var confirming by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reveal by remember { mutableStateOf(false) }

    // Auto-hide the revealed digits after 5 seconds — fast peek, not a leak.
    LaunchedEffect(reveal) {
        if (reveal) {
            delay(5_000)
            reveal = false
        }
    }

    val title = if (confirming) "Confirm your PIN" else "Set a PIN"
    val subtitle = if (confirming) {
        "Enter it again so we know we agree."
    } else {
        "Used to encrypt your data. If you forget it, your data is unrecoverable — there is no reset."
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.size(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(1f))

        // Dots or revealed digits — gives clear feedback on every tap.
        Box(Modifier.widthIn(min = 240.dp), contentAlignment = Alignment.Center) {
            if (reveal) {
                Text(
                    text = if (draft.isEmpty()) "—" else draft,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            } else {
                PinDotsRow(filled = draft.length, total = MIN_PIN_LENGTH)
            }
        }
        Spacer(Modifier.size(8.dp))

        // Reveal toggle — auto-hides after 5s. Helps users verify what they
        // entered without weakening shoulder-surfing resistance permanently.
        TextButton(onClick = { reveal = !reveal }) {
            Text(if (reveal) "Hide" else "Show")
        }

        Spacer(Modifier.size(8.dp))
        if (error != null) {
            Text(
                error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Text(
                "${draft.length} / $MIN_PIN_LENGTH+ digits",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.size(24.dp))
        PinKeypad(
            onDigit = { d ->
                if (draft.length < MAX_PIN_LENGTH) {
                    draft += d.toString()
                    error = null
                }
            },
            onBackspace = {
                if (draft.isNotEmpty()) draft = draft.dropLast(1)
                error = null
            },
        )
        Spacer(Modifier.size(16.dp))

        OnboardingPrimaryButton(
            text = if (confirming) "Confirm" else "Continue",
            enabled = draft.length >= MIN_PIN_LENGTH,
            onClick = {
                if (!confirming) {
                    if (draft.length < MIN_PIN_LENGTH) {
                        error = "PIN must be at least $MIN_PIN_LENGTH digits"
                    } else {
                        firstPin = draft
                        draft = ""
                        confirming = true
                        reveal = false
                    }
                } else {
                    if (draft == firstPin) {
                        onContinue(firstPin)
                    } else {
                        error = "PINs don't match. Start over."
                        firstPin = ""
                        draft = ""
                        confirming = false
                        reveal = false
                    }
                }
            },
        )
    }
}

@Composable
private fun PinDotsRow(filled: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        // Always show at least `total` dots; if user has entered more than the
        // minimum, render the extras so they can see how many they typed.
        val visible = maxOf(total, filled)
        repeat(visible) { i ->
            Surface(
                modifier = Modifier.size(16.dp),
                shape = CircleShape,
                color = if (i < filled) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                content = {},
            )
        }
    }
}
