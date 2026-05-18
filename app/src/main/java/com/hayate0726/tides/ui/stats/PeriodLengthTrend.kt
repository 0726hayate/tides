package com.hayate0726.tides.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun PeriodLengthTrend(values: List<Int>, modifier: Modifier = Modifier) {
    if (values.size < 2) {
        Text(
            "Need at least two completed cycles to show the trend.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = modifier.fillMaxWidth().height(72.dp)) {
        val w = size.width
        val h = size.height
        val maxV = (values.maxOrNull() ?: 1).coerceAtLeast(1)
        val minV = values.minOrNull() ?: 0
        val span = (maxV - minV).coerceAtLeast(1)
        val stepX = if (values.size > 1) w / (values.size - 1) else w
        val points = values.mapIndexed { i, v ->
            val x = i * stepX
            val y = h - ((v - minV).toFloat() / span) * (h * 0.85f) - (h * 0.075f)
            Offset(x, y)
        }
        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (p in points.drop(1)) lineTo(p.x, p.y)
        }
        drawPath(path = path, color = color,
                 style = Stroke(width = 4f, cap = StrokeCap.Round))
        for (p in points) drawCircle(color = color, radius = 6f, center = p)
    }
}
