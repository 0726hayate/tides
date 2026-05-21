package com.hayate0726.tides.ui.onboarding

import com.hayate0726.tides.domain.PhaseCalculator
import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.domain.model.Cycle
import com.hayate0726.tides.domain.model.Goal
import java.time.LocalDate

data class PredictionPreviewState(
    val enteredPeriod: ClosedRange<LocalDate>,
    val predictedNextPeriod: ClosedRange<LocalDate>,
    val fertileWindow: ClosedRange<LocalDate>?,
    val hormonalBcNote: Boolean,
)

object PredictionPreviewViewModel {

    private const val ASSUMED_CYCLE_LENGTH = 28
    private const val DEFAULT_PERIOD_LENGTH_DAYS = 5

    fun compute(
        lastPeriodStart: LocalDate,
        goals: Set<Goal>,
        birthControl: BirthControlMethod,
    ): PredictionPreviewState {
        val enteredPeriod = lastPeriodStart..lastPeriodStart.plusDays(
            (DEFAULT_PERIOD_LENGTH_DAYS - 1).toLong(),
        )
        val nextStart = lastPeriodStart.plusDays(ASSUMED_CYCLE_LENGTH.toLong())
        val predictedNextPeriod = nextStart..nextStart.plusDays(
            (DEFAULT_PERIOD_LENGTH_DAYS - 1).toLong(),
        )

        val syntheticActive = Cycle(
            start = lastPeriodStart,
            periodEnd = lastPeriodStart.plusDays((DEFAULT_PERIOD_LENGTH_DAYS - 1).toLong()),
            nextStart = null,
        )
        val phase = PhaseCalculator.compute(
            cycles = listOf(syntheticActive),
            today = lastPeriodStart,
            birthControl = birthControl,
            goals = goals,
            assumeCycleLength = ASSUMED_CYCLE_LENGTH,
        )
        val fertile = phase?.ovulationWindow?.let { it.start..it.end }

        return PredictionPreviewState(
            enteredPeriod = enteredPeriod,
            predictedNextPeriod = predictedNextPeriod,
            fertileWindow = fertile,
            hormonalBcNote = birthControl.isHormonal,
        )
    }
}
