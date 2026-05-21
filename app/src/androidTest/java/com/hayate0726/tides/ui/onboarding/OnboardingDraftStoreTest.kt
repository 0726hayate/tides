package com.hayate0726.tides.ui.onboarding

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.domain.model.FlowIntensity
import com.hayate0726.tides.domain.model.Goal
import com.hayate0726.tides.domain.model.Symptom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class OnboardingDraftStoreTest {

    private lateinit var store: OnboardingDraftStore

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        ctx.getSharedPreferences("onboarding_draft", android.content.Context.MODE_PRIVATE)
            .edit().clear().commit()
        store = OnboardingDraftStore(ctx)
    }

    @Test
    fun exists_returns_false_when_nothing_saved() {
        assertFalse(store.exists())
    }

    @Test
    fun round_trip_preserves_all_SafeDraft_fields() {
        val draft = SafeDraft(
            goals = setOf(Goal.TRACK_PERIOD, Goal.AVOID_PREGNANCY),
            birthControl = BirthControlMethod.CONDOM,
            biometricEnabled = false,
            lastPeriodStart = LocalDate.parse("2026-05-01"),
            flow = FlowIntensity.MEDIUM,
            symptoms = setOf(Symptom.CRAMPS, Symptom.BLOATING),
        )
        store.save(draft, OnboardingStep.LAST_PERIOD)
        val loaded = store.load()
        assertEquals(draft, loaded?.first)
        assertEquals(OnboardingStep.LAST_PERIOD, loaded?.second)
    }

    @Test
    fun drafts_older_than_7_days_fail_exists() {
        val draft = SafeDraft(
            goals = setOf(Goal.TRACK_PERIOD),
            birthControl = BirthControlMethod.NONE,
            biometricEnabled = true,
            lastPeriodStart = null,
            flow = null,
            symptoms = emptySet(),
        )
        val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
        val sp = ctx.getSharedPreferences("onboarding_draft", android.content.Context.MODE_PRIVATE)
        store.save(draft, OnboardingStep.GOALS)
        val eightDaysAgo = System.currentTimeMillis() - 8L * 24 * 60 * 60 * 1000
        sp.edit().putLong("createdAt", eightDaysAgo).commit()
        assertFalse(store.exists())
        assertNull(store.load())
    }

    @Test
    fun clear_removes_all_draft_data() {
        val draft = SafeDraft(
            goals = setOf(Goal.TRACK_PERIOD),
            birthControl = BirthControlMethod.NONE,
            biometricEnabled = true,
            lastPeriodStart = null,
            flow = null,
            symptoms = emptySet(),
        )
        store.save(draft, OnboardingStep.GOALS)
        assertTrue(store.exists())
        store.clear()
        assertFalse(store.exists())
        assertNull(store.load())
    }
}
