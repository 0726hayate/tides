package com.hayate0726.tides.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun PinKeypad(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        listOf(1, 2, 3),
        listOf(4, 5, 6),
        listOf(7, 8, 9),
    )
    Column(
        modifier = modifier.widthIn(max = 280.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                row.forEach { n -> KeyButton(n.toString(), Modifier.weight(1f)) { onDigit(n) } }
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
        modifier = modifier.aspectRatio(1.4f),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.headlineMedium)
        }
    }
}
