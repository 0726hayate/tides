package com.hayate0726.tides.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import com.hayate0726.tides.domain.model.Cycle
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes the widget summary file and pokes the Glance widget to redraw.
 *
 * Called from [com.hayate0726.tides.ui.calendar.CalendarViewModel] and
 * [com.hayate0726.tides.ui.log.LogViewModel] after data changes — i.e.
 * only while the app is unlocked. The widget renders from the on-disk
 * snapshot afterwards, with no access to the encrypted database.
 *
 * If no widget has been added to the home screen, [updateAll] is a no-op,
 * so writing the snapshot is still useful for the next time a widget is
 * installed.
 */
@Singleton
class WidgetUpdater @Inject constructor(
    @ApplicationContext private val ctx: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun publish(cycles: List<Cycle>, today: LocalDate = LocalDate.now()) {
        val day = WidgetSummary.computeCycleDay(today, cycles)
        WidgetSummary.write(
            ctx,
            WidgetSummary.Snapshot(
                cycleDay = day,
                updatedAtEpochMs = System.currentTimeMillis(),
            ),
        )
        scope.launch {
            runCatching {
                val mgr = GlanceAppWidgetManager(ctx)
                if (mgr.getGlanceIds(TidesDiscreetWidget::class.java).isNotEmpty()) {
                    TidesDiscreetWidget().updateAll(ctx)
                }
            }
        }
    }
}
