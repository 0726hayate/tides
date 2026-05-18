package com.hayate0726.tides.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayate0726.tides.data.TidesDatabase
import com.hayate0726.tides.domain.CycleDetector
import com.hayate0726.tides.domain.CycleStats
import com.hayate0726.tides.domain.SymptomStats
import com.hayate0726.tides.domain.model.Symptom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Loads the last 12 months of cycles and symptoms, computes stats + insight,
 * and exposes them as a [StatsUiState]. Same construction pattern as
 * [com.hayate0726.tides.ui.calendar.CalendarViewModel] — manual factory in
 * MainHost rather than Hilt, because the DB is opened post-unlock at runtime.
 */
class StatsViewModel(
    private val db: TidesDatabase,
) : ViewModel() {

    private val _state = MutableStateFlow<StatsUiState?>(null)
    val state: StateFlow<StatsUiState?> = _state.asStateFlow()

    private val _insightDismissed = MutableStateFlow(false)

    init { refresh() }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val to = LocalDate.now()
            val from = to.minusMonths(12)
            val entries = db.cycleEntryDao().rangeOnce(from, to)
            val cycles = CycleDetector.detect(
                entries.map { CycleDetector.Entry(it.date, it.flowIntensity) }
            )
            val cycleStats = CycleStats.compute(cycles)
            val symptomRows = db.symptomEntryDao().rangeOnce(from, to)
            val symptomStats = SymptomStats.compute(
                symptomRows.map { SymptomStats.Entry(it.date, it.symptom) },
                cycles,
            )

            val cycleLengths = cycles.mapNotNull { it.length }
            val cycleLabels = cycles.mapNotNull { c -> c.length?.let { c.start.month.name.take(3) } }

            val topByCycleDay: Map<Symptom, Int> = symptomStats.cycleDayHeatmap
                .mapValues { (_, days) -> days.maxByOrNull { it.value }?.key ?: 0 }
                .filterValues { it > 0 }

            val insight = if (_insightDismissed.value) null
                else InsightGenerator.generate(cycleStats, topByCycleDay)

            _state.value = StatsUiState(
                cycleStats = cycleStats,
                cycleLengthsForChart = cycleLengths,
                cycleLengthLabels = cycleLabels,
                symptomFrequency = symptomStats.frequency,
                insight = insight,
            )
        }
    }

    fun dismissInsight() {
        _insightDismissed.value = true
        _state.value = _state.value?.copy(insight = null)
    }
}
