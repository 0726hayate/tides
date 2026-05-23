package com.hayate0726.tides.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hayate0726.tides.domain.model.CalendarView

private val OPTIONS = listOf(
    CalendarView.ALL to "All",
    CalendarView.PERIOD_ONLY to "Period only",
    CalendarView.PHASES to "Phases",
    CalendarView.SYMPTOMS to "Symptoms",
)

// Approximate width the four chips + spacers need to render side-by-side.
// Below this, fall back to a dropdown so the row doesn't clip "Symptoms".
private val CHIP_ROW_MIN_WIDTH = 340.dp

@Composable
fun CalendarViewToggle(
    current: CalendarView,
    onChange: (CalendarView) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= CHIP_ROW_MIN_WIDTH) {
            ChipRow(current = current, onChange = onChange)
        } else {
            DropdownToggle(current = current, onChange = onChange)
        }
    }
}

@Composable
private fun ChipRow(current: CalendarView, onChange: (CalendarView) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OPTIONS.forEachIndexed { i, (view, label) ->
            val selected = view == current
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surfaceVariant),
                onClick = { onChange(view) },
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) MaterialTheme.colorScheme.background
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            if (i < OPTIONS.size - 1) Spacer(Modifier.size(6.dp))
        }
    }
}

@Composable
private fun DropdownToggle(current: CalendarView, onChange: (CalendarView) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = OPTIONS.first { it.first == current }.second

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.surfaceVariant),
            onClick = { expanded = true },
        ) {
            Text(
                "$currentLabel  ▾",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            OPTIONS.forEach { (view, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onChange(view)
                        expanded = false
                    },
                )
            }
        }
    }
}
