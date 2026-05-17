package com.hayate0726.tides.ui.calendar

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hayate0726.tides.domain.model.CalendarView

@Composable
fun CalendarViewToggle(
    current: CalendarView,
    onChange: (CalendarView) -> Unit,
) {
    val options = listOf(
        CalendarView.ALL to "All",
        CalendarView.PERIOD_ONLY to "Period only",
        CalendarView.PHASES to "Phases",
        CalendarView.SYMPTOMS to "Symptoms",
    )
    Row {
        options.forEachIndexed { i, (view, label) ->
            val selected = view == current
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (selected) MaterialTheme.colorScheme.onSurface
                        else androidx.compose.ui.graphics.Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(
                    1.5.dp, MaterialTheme.colorScheme.surfaceVariant,
                ),
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
            if (i < options.size - 1) Spacer(Modifier.size(6.dp))
        }
    }
}
