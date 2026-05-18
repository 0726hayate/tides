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
    private val biometricKeyStore: com.hayate0726.tides.crypto.BiometricKeyStore,
) : ViewModel() {

    data class DraftState(
        val goals: Set<Goal> = setOf(Goal.TRACK_PERIOD, Goal.TRACK_SYMPTOMS),
        val pin: String = "",
        val biometricEnabled: Boolean = true,
        val threatPreset: ThreatPreset = ThreatPreset.DEFAULT,
        val birthControl: BirthControlMethod = BirthControlMethod.NONE,
        val lastPeriodStart: LocalDate? = null,
    )

    private val _draft = MutableStateFlow(DraftState())
    val draft: StateFlow<DraftState> = _draft.asStateFlow()

    private val _completion = MutableStateFlow<TidesDatabase?>(null)
    val completion: StateFlow<TidesDatabase?> = _completion.asStateFlow()

    fun setGoals(goals: Set<Goal>) { _draft.value = _draft.value.copy(goals = goals) }
    fun setPin(pin: String) { _draft.value = _draft.value.copy(pin = pin) }
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

            val pin = Pin(draft.pin.toCharArray())
            val pinHash = KeyDerivation.derivePinHash(pin, pinHashSalt)
            val key = KeyDerivation.deriveKey(pin, keySalt)
            pin.zero()

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
            if (draft.biometricEnabled) {
                // Silently no-op on devices without biometric hardware. The Settings
                // toggle later can retry. We don't want onboarding to fail because the
                // emulator/device lacks a fingerprint reader.
                runCatching { biometricKeyStore.enroll(key) }
            }
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
