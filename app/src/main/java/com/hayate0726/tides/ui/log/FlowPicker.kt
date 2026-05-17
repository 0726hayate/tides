package com.hayate0726.tides.ui.log

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hayate0726.tides.domain.model.FlowIntensity
import com.hayate0726.tides.ui.theme.DropGlyph

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowPicker(
    selected: FlowIntensity?,
    onSelect: (FlowIntensity) -> Unit,
) {
    val options = listOf(
        Triple(FlowIntensity.NONE, "None", 0),
        Triple(FlowIntensity.SPOTTING, "Spotting", 1),
        Triple(FlowIntensity.LIGHT, "Light", 1),
        Triple(FlowIntensity.MEDIUM, "Medium", 2),
        Triple(FlowIntensity.HEAVY, "Heavy", 3),
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (intensity, label, glyphs) ->
            val isSelected = selected == intensity
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (isSelected) MaterialTheme.colorScheme.secondary
                        else Color.Transparent,
                border = BorderStroke(
                    1.5.dp,
                    if (isSelected) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.surfaceVariant,
                ),
                onClick = { onSelect(intensity) },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        label,
                        color = if (isSelected) MaterialTheme.colorScheme.onSecondary
                                else MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (glyphs > 0) {
                        Spacer(Modifier.size(6.dp))
                        repeat(glyphs) { i ->
                            DropGlyph(
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondary
                                        else MaterialTheme.colorScheme.onBackground,
                                size = 8.dp,
                            )
                            if (i < glyphs - 1) Spacer(Modifier.size(2.dp))
                        }
                    }
                }
            }
        }
    }
}
