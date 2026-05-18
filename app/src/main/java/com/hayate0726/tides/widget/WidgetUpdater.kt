package com.hayate0726.tides.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.hayate0726.tides.domain.CyclePredictor
import com.hayate0726.tides.domain.model.Cycle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun publish(
        cycles: List<Cycle>,
        showOvulation: Boolean = false,
        today: LocalDate = LocalDate.now(),
    ) {
        val cycleDay = WidgetSummary.computeCycleDay(today, cycles)
        val prediction = CyclePredictor.predictNextPeriod(cycles)
        val ovulationDay: Long? = if (showOvulation) computeOvulationEpochDay(cycles, today) else null

        WidgetSummary.write(
            ctx,
            WidgetSummary.Snapshot(
                cycleDay = cycleDay,
                updatedAtEpochMs = System.currentTimeMillis(),
                predictedPeriodStartEpochDay = prediction?.start?.toEpochDay(),
                showOvulation = showOvulation,
                ovulationDateEpochDay = ovulationDay,
            ),
        )
        scope.launch {
            runCatching {
                val mgr = GlanceAppWidgetManager(ctx)
                if (mgr.getGlanceIds(TidesDiscreetWidget::class.java).isNotEmpty()) {
                    TidesDiscreetWidget().updateAll(ctx)
                }
                if (mgr.getGlanceIds(TidesNormalWidget::class.java).isNotEmpty()) {
                    TidesNormalWidget().updateAll(ctx)
                }
            }
        }
    }

    /**
     * Estimates the ovulation date using the fixed-luteal heuristic:
     * ovulationDay = medianCycleLength - 14, counted from the active cycle start.
     * Returns null if there is no active cycle or no completed cycle lengths.
     */
    private fun computeOvulationEpochDay(cycles: List<Cycle>, today: LocalDate): Long? {
        val active = cycles.firstOrNull { it.isActive } ?: return null
        val completedLengths = cycles.filter { !it.isActive }.mapNotNull { it.length }
        if (completedLengths.isEmpty()) return null
        val sorted = completedLengths.sorted()
        val median = sorted[(sorted.size - 1) / 2]
        // Trust range matches PhaseCalculator.compute (spec §5.5): the
        // fixed-luteal heuristic is unreliable outside a typical cycle window,
        // so we suppress the widget's fertile-window line for users whose
        // median cycle length is below 21 or above 35 days.
        if (median !in 21..35) return null
        val ovulationDayOfCycle = median - 14
        return active.start.plusDays((ovulationDayOfCycle - 1).toLong()).toEpochDay()
    }
}
