package com.hayate0726.tides.ui.onboarding

import android.content.Context
import android.content.SharedPreferences
import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.domain.model.FlowIntensity
import com.hayate0726.tides.domain.model.Goal
import com.hayate0726.tides.domain.model.Symptom
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingDraftStore @Inject constructor(
    @ApplicationContext ctx: Context,
) {
    private val sp: SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun exists(): Boolean = sp.contains(KEY_CREATED_AT) && !isExpired()

    fun save(draft: SafeDraft, step: OnboardingStep) {
        sp.edit()
            .putString(KEY_GOALS, draft.goals.joinToString(",") { it.name })
            .putString(KEY_BC, draft.birthControl.name)
            .putBoolean(KEY_BIOMETRIC, draft.biometricEnabled)
            .putString(KEY_LAST_PERIOD, draft.lastPeriodStart?.toString() ?: "")
            .putString(KEY_FLOW, draft.flow?.name ?: "")
            .putString(KEY_SYMPTOMS, draft.symptoms.joinToString(",") { it.name })
            .putString(KEY_STEP, step.name)
            .putLong(KEY_CREATED_AT, System.currentTimeMillis())
            .apply()
    }

    fun load(): Pair<SafeDraft, OnboardingStep>? {
        if (!exists()) return null
        val goals = sp.getString(KEY_GOALS, "")!!
            .split(",").filter { it.isNotBlank() }
            .mapNotNull { runCatching { Goal.valueOf(it) }.getOrNull() }
            .toSet()
        val bc = runCatching { BirthControlMethod.valueOf(sp.getString(KEY_BC, "NONE")!!) }
            .getOrDefault(BirthControlMethod.NONE)
        val biometric = sp.getBoolean(KEY_BIOMETRIC, true)
        val lastPeriodRaw = sp.getString(KEY_LAST_PERIOD, "")!!
        val lastPeriod = if (lastPeriodRaw.isBlank()) null
            else runCatching { LocalDate.parse(lastPeriodRaw) }.getOrNull()
        val flowRaw = sp.getString(KEY_FLOW, "")!!
        val flow = if (flowRaw.isBlank()) null
            else runCatching { FlowIntensity.valueOf(flowRaw) }.getOrNull()
        val symptoms = sp.getString(KEY_SYMPTOMS, "")!!
            .split(",").filter { it.isNotBlank() }
            .mapNotNull { runCatching { Symptom.valueOf(it) }.getOrNull() }
            .toSet()
        val step = runCatching { OnboardingStep.valueOf(sp.getString(KEY_STEP, "WELCOME")!!) }
            .getOrDefault(OnboardingStep.WELCOME)
        return SafeDraft(
            goals = goals,
            birthControl = bc,
            biometricEnabled = biometric,
            lastPeriodStart = lastPeriod,
            flow = flow,
            symptoms = symptoms,
        ) to step
    }

    fun clear() {
        sp.edit().clear().apply()
    }

    private fun isExpired(): Boolean {
        val createdAt = sp.getLong(KEY_CREATED_AT, 0L)
        if (createdAt == 0L) return true
        return System.currentTimeMillis() - createdAt > MAX_AGE_MS
    }

    companion object {
        private const val PREFS_NAME = "onboarding_draft"
        private const val KEY_GOALS = "goals"
        private const val KEY_BC = "bc"
        private const val KEY_BIOMETRIC = "biometric"
        private const val KEY_LAST_PERIOD = "lastPeriod"
        private const val KEY_FLOW = "flow"
        private const val KEY_SYMPTOMS = "symptoms"
        private const val KEY_STEP = "step"
        private const val KEY_CREATED_AT = "createdAt"
        private const val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000
    }
}

data class SafeDraft(
    val goals: Set<Goal>,
    val birthControl: BirthControlMethod,
    val biometricEnabled: Boolean,
    val lastPeriodStart: LocalDate?,
    val flow: FlowIntensity?,
    val symptoms: Set<Symptom>,
)
