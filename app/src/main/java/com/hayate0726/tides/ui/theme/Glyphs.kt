package com.hayate0726.tides.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Tiny drop shape used on period circles (CVD-safe glyph per spec §6).
 *
 *   ◤
 *  ◤ ◢
 *   ◣
 *
 * Drawn as a rounded square rotated -45°.
 */
@Composable
fun DropGlyph(
    color: Color,
    size: Dp = 6.dp,
    modifier: Modifier = Modifier,
    /**
     * When false, draws only the outline. Use for *predicted* period days
     * so users can distinguish them from logged (filled) period days.
     */
    filled: Boolean = true,
    contentDescription: String? = null,
) {
    val desc = contentDescription
    Canvas(modifier = modifier.size(size).let { if (desc != null) it.semantics { this.contentDescription = desc } else it }) {
        val w = this.size.minDimension
        val path = Path().apply {
            moveTo(w * 0.5f, 0f)
            cubicTo(w * 0.9f, w * 0.2f, w, w * 0.5f, w * 0.5f, w)
            cubicTo(0f, w * 0.5f, w * 0.1f, w * 0.2f, w * 0.5f, 0f)
            close()
        }
        if (filled) {
            drawPath(path, color = color)
        } else {
            drawPath(path, color = color, style = Stroke(width = w * 0.18f))
        }
    }
}

/**
 * Diamond glyph used on ovulation rings.
 */
@Composable
fun DiamondGlyph(
    color: Color,
    size: Dp = 6.dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val desc = contentDescription
    Canvas(modifier = modifier.size(size).let { if (desc != null) it.semantics { this.contentDescription = desc } else it }) {
        val w = this.size.minDimension
        val path = Path().apply {
            moveTo(w * 0.5f, 0f)
            lineTo(w, w * 0.5f)
            lineTo(w * 0.5f, w)
            lineTo(0f, w * 0.5f)
            close()
        }
        drawPath(path, color = color)
    }
}

/**
 * Dashed horizontal bar used for predicted-period range under the calendar.
 * Renders below the row so it is structurally distinct from day circles.
 */
@Composable
fun DashedBar(
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 3.dp,
    contentDescription: String? = null,
) {
    val desc = contentDescription
    // Caller's modifier wins (so it can fillMaxWidth or set a specific width);
    // height is applied last so it always takes effect.
    Canvas(modifier = modifier.fillMaxWidth().height(height).let { if (desc != null) it.semantics { this.contentDescription = desc } else it }) {
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(0f, this.size.height / 2),
            end = androidx.compose.ui.geometry.Offset(this.size.width, this.size.height / 2),
            strokeWidth = this.size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
        )
    }
}
