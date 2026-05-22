package com.hayate0726.tides.data

import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.domain.model.Goal
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UserPrivacyViewTest {

    @Test fun avoid_pregnancy_with_no_bc_shows_ovulation() {
        val v = UserPrivacyView.compute(setOf(Goal.AVOID_PREGNANCY), activeBc = null)
        assertTrue(v.showOvulation)
    }

    @Test fun ttc_with_copper_iud_shows_ovulation() {
        val v = UserPrivacyView.compute(setOf(Goal.TRYING_TO_CONCEIVE), BirthControlMethod.COPPER_IUD)
        assertTrue(v.showOvulation)
    }

    @Test fun track_period_alone_with_no_bc_now_shows_ovulation() {
        // v1.3 removed the goal-based gate — ovulation UI surfaces for
        // anyone whose active BC isn't hormonal, regardless of goals.
        val v = UserPrivacyView.compute(setOf(Goal.TRACK_PERIOD), activeBc = null)
        assertTrue(v.showOvulation)
    }

    @Test fun avoid_pregnancy_with_pill_suppresses_ovulation() {
        val v = UserPrivacyView.compute(setOf(Goal.AVOID_PREGNANCY), BirthControlMethod.PILL)
        assertFalse(v.showOvulation)
    }

    @Test fun avoid_pregnancy_with_hormonal_iud_suppresses_ovulation() {
        val v = UserPrivacyView.compute(setOf(Goal.AVOID_PREGNANCY), BirthControlMethod.HORMONAL_IUD)
        assertFalse(v.showOvulation)
    }

    @Test fun no_goals_with_no_bc_now_shows_ovulation() {
        // v1.3: no-goals + no-BC users see predictions too.
        val v = UserPrivacyView.compute(emptySet(), activeBc = null)
        assertTrue(v.showOvulation)
    }
}
