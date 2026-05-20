package com.hayate0726.tides.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

internal fun shuffledDigitOrder(): List<Int> = (1..9).shuffled()

@Composable
fun ShuffledPinKeypad(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val order = remember { shuffledDigitOrder() }
    Column(
        modifier = modifier.widthIn(max = 280.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        for (rowIdx in 0..2) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                for (colIdx in 0..2) {
                    val n = order[rowIdx * 3 + colIdx]
                    KeyButton(n.toString(), Modifier.weight(1f)) { onDigit(n) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.weight(1f))
            KeyButton("0", Modifier.weight(1f)) { onDigit(0) }
            KeyButton(
                "⌫",
                Modifier.weight(1f).semantics { contentDescription = "Backspace" },
                onClick = onBackspace,
            )
        }
    }
}

@Composable
private fun KeyButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp).aspectRatio(1f),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.headlineMedium)
        }
    }
}
