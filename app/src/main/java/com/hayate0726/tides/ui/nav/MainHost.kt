package com.hayate0726.tides.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hayate0726.tides.AppState
import com.hayate0726.tides.AppViewModel
import com.hayate0726.tides.data.TidesDatabase
import com.hayate0726.tides.ui.calendar.CalendarMonthState
import com.hayate0726.tides.ui.calendar.CalendarScreen
import com.hayate0726.tides.ui.calendar.CalendarViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private class CalendarViewModelFactory(
    private val db: TidesDatabase,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CalendarViewModel::class.java))
        return CalendarViewModel(db) as T
    }
}

/**
 * Main route host. Reads the unlocked TidesDatabase off AppState, exposes
 * it via LocalTidesDatabase, and renders the Calendar as the home screen.
 *
 * Plan 4 will add bottom navigation for Calendar / Stats / Settings and
 * the modal Log bottom sheet on day-tap. For now Calendar is the entire
 * Main route.
 */
@Composable
fun MainHost(appViewModel: AppViewModel) {
    val state by appViewModel.state.collectAsStateWithLifecycle()
    val db = when (val s = state) {
        is AppState.Unlocked -> s.db
        is AppState.UnlockedDecoy -> s.db
        else -> null
    }
    if (db == null) {
        // Transient — AppState transitioned away from unlocked. The
        // LaunchedEffect in TidesNavHost will route us elsewhere shortly.
        return
    }
    CompositionLocalProvider(LocalTidesDatabase provides db) {
        // Key by db identity so the VM is replaced (and the old one cleared
        // via onCleared -> viewModelScope.cancel) when the DB changes.
        val vm: CalendarViewModel = viewModel(
            key = "calendar-${System.identityHashCode(db)}",
            factory = CalendarViewModelFactory(db),
        )
        val ui by vm.state.collectAsStateWithLifecycle()
        Box(modifier = Modifier.fillMaxSize()) {
            val periodDays = remember(ui.cycles) {
                ui.cycles.flatMap { c ->
                    val end = c.periodEnd ?: c.start
                    val days = ChronoUnit.DAYS.between(c.start, end).toInt()
                    (0..days).map { c.start.plusDays(it.toLong()) }
                }.toSet()
            }
            CalendarScreen(
                monthState = CalendarMonthState(
                    month = ui.month,
                    today = ui.today,
                    periodDays = periodDays,
                    ovulationWindow = null,
                    predictedPeriod = null,
                    symptomDays = ui.symptomDays,
                ),
                view = ui.view,
                onViewChange = vm::changeView,
                onDayClick = { /* log sheet wiring comes in Plan 4 */ },
            )
            TextButton(
                onClick = { appViewModel.lock() },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            ) {
                Text("Lock", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
