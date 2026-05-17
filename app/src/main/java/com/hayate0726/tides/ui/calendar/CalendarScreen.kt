package com.hayate0726.tides.ui.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hayate0726.tides.domain.model.CalendarView
import java.time.LocalDate

@Composable
fun CalendarScreen(
    monthState: CalendarMonthState,
    view: CalendarView,
    onViewChange: (CalendarView) -> Unit,
    onDayClick: (LocalDate) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                monthState.month.month.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                " ${monthState.month.year}",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.size(20.dp))
        CalendarViewToggle(current = view, onChange = onViewChange)
        Spacer(Modifier.size(16.dp))
        CalendarMonth(
            state = monthState,
            view = view,
            onDayClick = onDayClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
