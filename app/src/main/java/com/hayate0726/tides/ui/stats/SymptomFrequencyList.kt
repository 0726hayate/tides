package com.hayate0726.tides.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hayate0726.tides.domain.model.Symptom

@Composable
fun SymptomFrequencyList(frequency: Map<Symptom, Int>) {
    if (frequency.isEmpty()) return
    val max = frequency.values.max()
    Column {
        frequency.entries.sortedByDescending { it.value }.take(6).forEach { (sym, count) ->
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) {
                Text(
                    sym.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() },
                    modifier = Modifier.padding(end = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Box(modifier = Modifier.weight(1f).height(8.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))) {
                    Box(modifier = Modifier
                        .fillMaxWidth(count / max.toFloat())
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.onSurface, RoundedCornerShape(4.dp)))
                }
                Text(count.toString(), modifier = Modifier.padding(start = 10.dp),
                     style = MaterialTheme.typography.labelSmall,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
