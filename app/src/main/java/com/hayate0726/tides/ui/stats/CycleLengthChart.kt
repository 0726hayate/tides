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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun CycleLengthChart(values: List<Int>, labels: List<String>) {
    if (values.isEmpty()) return
    val maxVal = values.max().toFloat()
    Row(modifier = Modifier.fillMaxWidth().height(110.dp)) {
        values.forEachIndexed { i, v ->
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((v / maxVal * 100).dp)
                        .background(MaterialTheme.colorScheme.secondary,
                                   RoundedCornerShape(4.dp, 4.dp, 0.dp, 0.dp)),
                    contentAlignment = androidx.compose.ui.Alignment.BottomCenter,
                ) {
                    Text(v.toString(), color = MaterialTheme.colorScheme.onSecondary,
                         style = MaterialTheme.typography.labelSmall,
                         modifier = Modifier.padding(bottom = 4.dp))
                }
            }
        }
    }
    Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        labels.forEach { l ->
            Text(l, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                 style = MaterialTheme.typography.labelSmall,
                 color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
