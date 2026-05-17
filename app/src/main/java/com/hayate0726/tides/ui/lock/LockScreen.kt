package com.hayate0726.tides.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun LockScreen(
    pinLength: Int,
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onBiometric: (() -> Unit)?,
    error: String?,
    cooldownExpiryEpochMs: Long?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "TIDES",
                style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 4.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Box(Modifier.size(0.dp, 40.dp))

            PinDots(filled = pinLength, total = 6)
            Box(Modifier.size(0.dp, 16.dp))

            Text(
                text = when {
                    cooldownExpiryEpochMs != null -> "Try again later"
                    error != null -> error
                    else -> "Enter your PIN"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (error != null) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Box(Modifier.size(0.dp, 24.dp))

            if (cooldownExpiryEpochMs == null) {
                PinKeypad(onDigit = onDigit, onBackspace = onBackspace)
            } else {
                CooldownCountdown(cooldownExpiryEpochMs)
            }

            if (onBiometric != null && cooldownExpiryEpochMs == null) {
                Box(Modifier.size(0.dp, 24.dp))
                TextButton(onClick = onBiometric) {
                    Text("Use biometric")
                }
            }
        }
    }
}

@Composable
private fun PinDots(filled: Int, total: Int) {
    Row(horizontalArrangement = spacedBy(16.dp)) {
        repeat(total) { i ->
            Surface(
                modifier = Modifier.size(14.dp),
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

@Composable
private fun CooldownCountdown(expiryEpochMs: Long) {
    var remaining by remember { mutableStateOf(maxOf(0L, expiryEpochMs - System.currentTimeMillis())) }
    LaunchedEffect(expiryEpochMs) {
        while (remaining > 0) {
            delay(1_000)
            remaining = maxOf(0L, expiryEpochMs - System.currentTimeMillis())
        }
    }
    val sec = (remaining / 1000).toInt()
    Text(
        "Wait %d:%02d".format(sec / 60, sec % 60),
        style = MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
