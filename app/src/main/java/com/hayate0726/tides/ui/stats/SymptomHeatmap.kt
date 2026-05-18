package com.hayate0726.tides.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hayate0726.tides.domain.model.Symptom
import kotlin.math.ln

/**
 * Per top-6 symptom, one row of 28 cells (cycle days 1..28). Cell opacity
 * scales with log(count). Empty cells are very faint so the grid remains
 * readable.
 */
@Composable
fun SymptomHeatmap(
    heatmap: Map<Symptom, Map<Int, Int>>,
    modifier: Modifier = Modifier,
) {
    if (heatmap.isEmpty()) {
        Text(
            "Log a few symptoms to see your patterns.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val top = heatmap.entries
        .sortedByDescending { (_, m) -> m.values.sum() }
        .take(6)
    val base = MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier) {
        for ((symptom, days) in top) {
            val maxV = days.values.max()
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()) {
                Text(
                    symptom.name.lowercase().replace('_', ' ')
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(110.dp),
                )
                for (cycleDay in 1..28) {
                    val v = days[cycleDay] ?: 0
                    val alpha = if (v == 0) 0.07f else
                        (0.3f + 0.7f * (ln(v + 1.0).toFloat() / ln(maxV + 1.0).toFloat()))
                            .coerceIn(0.3f, 1.0f)
                    Box(
                        modifier = Modifier
                            .size(width = 9.dp, height = 14.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(base.copy(alpha = alpha)),
                    )
                    if (cycleDay != 28) Spacer(Modifier.size(1.dp))
                }
            }
            Spacer(Modifier.size(6.dp))
        }
    }
}
