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
) : ViewModel() {

    data class DraftState(
        val goals: Set<Goal> = setOf(Goal.TRACK_PERIOD, Goal.TRACK_SYMPTOMS),
        val pinChars: CharArray? = null,
        val biometricEnabled: Boolean = true,
        val threatPreset: ThreatPreset = ThreatPreset.DEFAULT,
        val birthControl: BirthControlMethod = BirthControlMethod.NONE,
        val lastPeriodStart: LocalDate? = null,
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
            return true
        }
        override fun hashCode(): Int {
            var r = goals.hashCode()
            r = 31 * r + (pinChars?.toList()?.hashCode() ?: 0)
            r = 31 * r + biometricEnabled.hashCode()
            r = 31 * r + threatPreset.hashCode()
            r = 31 * r + birthControl.hashCode()
            r = 31 * r + (lastPeriodStart?.hashCode() ?: 0)
            return r
        }
    }

    private val _draft = MutableStateFlow(DraftState())
    val draft: StateFlow<DraftState> = _draft.asStateFlow()

    private val _completion = MutableStateFlow<TidesDatabase?>(null)
    val completion: StateFlow<TidesDatabase?> = _completion.asStateFlow()

    fun setGoals(goals: Set<Goal>) { _draft.value = _draft.value.copy(goals = goals) }
    fun setPin(pin: String) {
        val chars = pin.toCharArray()
        _draft.value = _draft.value.copy(pinChars = chars)
        // pin (String) goes out of scope and is eligible for GC at the JVM's
        // discretion. We accept the residual String pool hit during PinSetupScreen
        // composition — eliminating it requires a custom text-input component,
        // out of scope for v1.0. Lifetime: until complete() runs and zeros chars.
    }
    fun setBiometric(on: Boolean) { _draft.value = _draft.value.copy(biometricEnabled = on) }
    fun setThreatPreset(p: ThreatPreset) { _draft.value = _draft.value.copy(threatPreset = p) }
    fun setBc(m: BirthControlMethod) { _draft.value = _draft.value.copy(birthControl = m) }
    fun setLastPeriodStart(d: LocalDate?) { _draft.value = _draft.value.copy(lastPeriodStart = d) }

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
                        // LIGHT is the minimal non-zero flow; user only confirmed a
                        // start date during onboarding, not an intensity. They can
                        // edit it later from the calendar. MEDIUM (the previous
                        // hardcoded value) silently overstated their data.
                        flowIntensity = FlowIntensity.LIGHT,
                        painSeverity = null,
                        notes = null,
                    )
                )
            }
            _completion.value = db
        }
    }
}
