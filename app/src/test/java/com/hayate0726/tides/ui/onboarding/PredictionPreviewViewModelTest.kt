package com.hayate0726.tides.ui.onboarding

import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.domain.model.Goal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class PredictionPreviewViewModelTest {

    private val start = LocalDate.parse("2026-05-01")

    @Test
    fun `entered period spans 5 days from start`() {
        val state = PredictionPreviewViewModel.compute(
            lastPeriodStart = start,
            goals = setOf(Goal.AVOID_PREGNANCY),
            birthControl = BirthControlMethod.NONE,
        )
        assertEquals(start, state.enteredPeriod.start)
        assertEquals(start.plusDays(4), state.enteredPeriod.endInclusive)
    }

    @Test
    fun `predicted next period starts 28 days after entered start`() {
        val state = PredictionPreviewViewModel.compute(
            lastPeriodStart = start,
            goals = setOf(Goal.AVOID_PREGNANCY),
            birthControl = BirthControlMethod.NONE,
        )
        assertEquals(start.plusDays(28), state.predictedNextPeriod.start)
        assertEquals(start.plusDays(32), state.predictedNextPeriod.endInclusive)
    }

    @Test
    fun `fertile window present when goals allow and bc non-hormonal`() {
        val state = PredictionPreviewViewModel.compute(
            lastPeriodStart = start,
            goals = setOf(Goal.AVOID_PREGNANCY),
            birthControl = BirthControlMethod.NONE,
        )
        assertNotNull(state.fertileWindow)
        assertEquals(start.plusDays(11), state.fertileWindow!!.start)
        assertEquals(start.plusDays(15), state.fertileWindow!!.endInclusive)
    }

    @Test
    fun `fertile window absent when goals dont include ovulation-relevant`() {
        val state = PredictionPreviewViewModel.compute(
            lastPeriodStart = start,
            goals = setOf(Goal.TRACK_PERIOD),
            birthControl = BirthControlMethod.NONE,
        )
        assertNull(state.fertileWindow)
    }

    @Test
    fun `fertile window absent when bc is hormonal`() {
        val state = PredictionPreviewViewModel.compute(
            lastPeriodStart = start,
            goals = setOf(Goal.AVOID_PREGNANCY),
            birthControl = BirthControlMethod.PILL,
        )
        assertNull(state.fertileWindow)
    }
}
