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

class StatsViewModel(
    private val db: TidesDatabase,
) : ViewModel() {

    enum class Range(val months: Int?) {
        THREE_MO(3),
        SIX_MO(6),
        ONE_YR(12),
        ALL(null),
    }

    private val _state = MutableStateFlow<StatsUiState?>(null)
    val state: StateFlow<StatsUiState?> = _state.asStateFlow()

    private val _range = MutableStateFlow(Range.SIX_MO)
    val range: StateFlow<Range> = _range.asStateFlow()

    private val _insightDismissed = MutableStateFlow(false)

    init { refresh() }

    fun setRange(r: Range) {
        if (_range.value == r) return
        _range.value = r
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val to = LocalDate.now()
            val from = _range.value.months?.let { to.minusMonths(it.toLong()) }
                ?: LocalDate.of(2000, 1, 1)
            val entries = db.cycleEntryDao().rangeOnce(from, to)
            val activeBc = db.birthControlDao().activeOnce()?.method
            val cycles = CycleDetector.detect(
                entries.map { CycleDetector.Entry(it.date, it.flowIntensity) },
                activeBirthControl = activeBc,
            )
            val cycleStats = CycleStats.compute(cycles)
            val symptomRows = db.symptomEntryDao().rangeOnce(from, to)
            val symptomStats = SymptomStats.compute(
                symptomRows.map { SymptomStats.Entry(it.date, it.symptom) },
                cycles,
            )

            val cycleLengths = cycles.mapNotNull { it.length }
            val cycleLabels = cycles.mapNotNull { c -> c.length?.let { c.start.month.name.take(3) } }
            val periodLengths = cycles.mapNotNull { it.periodLength }

            val topByCycleDay: Map<Symptom, Int> = symptomStats.cycleDayHeatmap
                .mapValues { (_, days) -> days.maxByOrNull { it.value }?.key ?: 0 }
                .filterValues { it > 0 }

            val insight = if (_insightDismissed.value) null
                else InsightGenerator.generate(cycleStats, topByCycleDay)

            _state.value = StatsUiState(
                cycleStats = cycleStats,
                cycleLengthsForChart = cycleLengths,
                cycleLengthLabels = cycleLabels,
                periodLengthsForChart = periodLengths,
                symptomFrequency = symptomStats.frequency,
                symptomHeatmap = symptomStats.cycleDayHeatmap,
                insight = insight,
            )
        }
    }

    fun dismissInsight() {
        _insightDismissed.value = true
        _state.value = _state.value?.copy(insight = null)
    }
}
