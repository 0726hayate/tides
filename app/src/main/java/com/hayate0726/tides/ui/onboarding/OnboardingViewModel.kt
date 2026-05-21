package com.hayate0726.tides.ui.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hayate0726.tides.crypto.AuthMeta
import com.hayate0726.tides.crypto.FileAuthMetaStore
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import com.hayate0726.tides.data.DatabaseFactory
import com.hayate0726.tides.data.TidesDatabase
import com.hayate0726.tides.data.entity.BirthControlEntity
import com.hayate0726.tides.data.entity.CycleEntryEntity
import com.hayate0726.tides.data.entity.GoalEntity
import com.hayate0726.tides.data.entity.SettingsEntity
import com.hayate0726.tides.di.CyclesDbFile
import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.domain.model.FlowIntensity
import com.hayate0726.tides.domain.model.Goal
import com.hayate0726.tides.domain.model.ThreatPreset
import com.hayate0726.tides.domain.model.Symptom
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.security.SecureRandom
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context,
    @CyclesDbFile private val dbFile: File,
    private val authMetaStore: FileAuthMetaStore,
    internal val draftStore: OnboardingDraftStore,
) : ViewModel() {

    data class DraftState(
        val goals: Set<Goal> = setOf(Goal.TRACK_PERIOD, Goal.TRACK_SYMPTOMS),
        val pinChars: CharArray? = null,
        val biometricEnabled: Boolean = true,
        val threatPreset: ThreatPreset = ThreatPreset.DEFAULT,
        val birthControl: BirthControlMethod = BirthControlMethod.NONE,
        val lastPeriodStart: LocalDate? = null,
        val flow: FlowIntensity? = null,
        val symptoms: Set<Symptom> = emptySet(),
    ) {
        // CharArray's equals is identity-based; for the StateFlow change-detection
        // we want value semantics so re-emitting the same draft doesn't churn.
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DraftState) return false
            if (goals != other.goals) return false
            if (pinChars?.toList() != other.pinChars?.toList()) return false
            if (biometricEnabled != other.biometricEnabled) return false
            if (threatPreset != other.threatPreset) return false
            if (birthControl != other.birthControl) return false
            if (lastPeriodStart != other.lastPeriodStart) return false
            if (flow != other.flow) return false
            if (symptoms != other.symptoms) return false
            return true
        }
        override fun hashCode(): Int {
            var r = goals.hashCode()
            r = 31 * r + (pinChars?.toList()?.hashCode() ?: 0)
            r = 31 * r + biometricEnabled.hashCode()
            r = 31 * r + threatPreset.hashCode()
            r = 31 * r + birthControl.hashCode()
            r = 31 * r + (lastPeriodStart?.hashCode() ?: 0)
            r = 31 * r + (flow?.hashCode() ?: 0)
            r = 31 * r + symptoms.hashCode()
            return r
        }
    }

    private val _draft = MutableStateFlow(DraftState())
    val draft: StateFlow<DraftState> = _draft.asStateFlow()

    private val _currentStep = MutableStateFlow(OnboardingStep.WELCOME)
    val currentStep: StateFlow<OnboardingStep> = _currentStep.asStateFlow()

    private val _completion = MutableStateFlow<TidesDatabase?>(null)
    val completion: StateFlow<TidesDatabase?> = _completion.asStateFlow()

    private var saveJob: kotlinx.coroutines.Job? = null
    private fun schedulePersist(step: OnboardingStep) {
        _currentStep.value = step
        saveJob?.cancel()
        saveJob = viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(200)
            val d = _draft.value
            draftStore.save(
                SafeDraft(
                    goals = d.goals,
                    birthControl = d.birthControl,
                    biometricEnabled = d.biometricEnabled,
                    lastPeriodStart = d.lastPeriodStart,
                    flow = d.flow,
                    symptoms = d.symptoms,
                ),
                step,
            )
        }
    }

    fun setGoals(goals: Set<Goal>) {
        _draft.value = _draft.value.copy(goals = goals)
        schedulePersist(OnboardingStep.PIN)
    }
    fun setPin(pin: String) {
        val chars = pin.toCharArray()
        _draft.value = _draft.value.copy(pinChars = chars)
        // pin (String) goes out of scope and is eligible for GC at the JVM's
        // discretion. We accept the residual String pool hit during PinSetupScreen
        // composition — eliminating it requires a custom text-input component,
        // out of scope for v1.0. Lifetime: until complete() runs and zeros chars.
        // NEVER persist PIN material. Bump step but no save.
        _currentStep.value = OnboardingStep.BIOMETRIC
    }
    fun setBiometric(on: Boolean) {
        _draft.value = _draft.value.copy(biometricEnabled = on)
        schedulePersist(OnboardingStep.THREAT)
    }
    fun setThreatPreset(p: ThreatPreset) {
        _draft.value = _draft.value.copy(threatPreset = p)
        // threatPreset deliberately not persisted — re-prompt on resume.
        _currentStep.value = OnboardingStep.LAST_PERIOD
    }
    fun setBc(m: BirthControlMethod) { _draft.value = _draft.value.copy(birthControl = m) }
    fun setLastPeriodStart(d: LocalDate?) {
        _draft.value = _draft.value.copy(lastPeriodStart = d)
        schedulePersist(OnboardingStep.FLOW_SYMPTOMS)
    }

    fun setFlow(f: com.hayate0726.tides.domain.model.FlowIntensity) {
        _draft.value = _draft.value.copy(flow = f)
        schedulePersist(OnboardingStep.PREDICTION)
    }

    fun setSymptoms(s: Set<com.hayate0726.tides.domain.model.Symptom>) {
        _draft.value = _draft.value.copy(symptoms = s)
        schedulePersist(OnboardingStep.PREDICTION)
    }

    fun resumeFromDraft() {
        val loaded = draftStore.load() ?: return
        _draft.value = _draft.value.copy(
            goals = loaded.first.goals,
            birthControl = loaded.first.birthControl,
            biometricEnabled = loaded.first.biometricEnabled,
            lastPeriodStart = loaded.first.lastPeriodStart,
            flow = loaded.first.flow,
            symptoms = loaded.first.symptoms,
            // threatPreset stays at the in-memory default — user re-prompts
        )
        _currentStep.value = loaded.second
    }

    fun startFresh() {
        draftStore.clear()
        _draft.value = DraftState()
        _currentStep.value = OnboardingStep.WELCOME
    }

    fun complete() {
        viewModelScope.launch(Dispatchers.IO) {
            val draft = _draft.value
            val rng = SecureRandom()
            val keySalt = ByteArray(16).also(rng::nextBytes)
            val pinHashSalt = ByteArray(16).also(rng::nextBytes)

            val pinChars = draft.pinChars ?: error("PIN not set")
            val pin = Pin(pinChars.copyOf())
            val pinHash = KeyDerivation.derivePinHash(pin, pinHashSalt)
            val key = KeyDerivation.deriveKey(pin, keySalt)
            pin.zero()
            java.util.Arrays.fill(pinChars, 0.toChar())
            _draft.value = _draft.value.copy(pinChars = null)

            authMetaStore.initialize(
                AuthMeta(
                    keySalt = keySalt,
                    pinHashSalt = pinHashSalt,
                    pinHash = pinHash,
                    duress = null,
                    failCount = 0,
                    cooldownExpiryEpochMs = 0L,
                )
            )

            val db = DatabaseFactory.open(ctx, dbFile, key)
            // Biometric enrollment requires a live BiometricPrompt session to authorize
            // the Keystore cipher.init(ENCRYPT_MODE) op, and onboarding can't show one
            // from a ViewModel context. The user's biometric_enabled choice is persisted
            // below; actual enrollment happens via Settings -> Biometric unlock after
            // onboarding completes.
            key.zero()

            // Seed the DB with onboarding choices
            for (g in draft.goals) db.goalDao().insert(GoalEntity(g))
            db.settingsDao().upsert(SettingsEntity("threat_preset", draft.threatPreset.name))
            db.settingsDao().upsert(SettingsEntity("biometric_enabled", draft.biometricEnabled.toString()))
            db.birthControlDao().insert(
                BirthControlEntity(
                    method = draft.birthControl,
                    startDate = LocalDate.now(),
                    endDate = null,
                )
            )
            draft.lastPeriodStart?.let { lpd ->
                db.cycleEntryDao().upsert(
                    CycleEntryEntity(
                        date = lpd,
                        flowIntensity = draft.flow ?: FlowIntensity.LIGHT,
                        painSeverity = null,
                        notes = null,
                    )
                )
                for (sym in draft.symptoms) {
                    db.symptomEntryDao().insert(
                        com.hayate0726.tides.data.entity.SymptomEntryEntity(
                            date = lpd,
                            symptom = sym,
                            severity = 1,
                            otherText = null,
                        )
                    )
                }
            }
            _completion.value = db
            draftStore.clear()
        }
    }
}
