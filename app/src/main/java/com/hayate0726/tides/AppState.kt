package com.hayate0726.tides

import com.hayate0726.tides.data.TidesDatabase

sealed interface AppState {
    /** Initial state while AppViewModel checks auth_meta.bin on disk. */
    data object Loading : AppState

    /** No auth_meta.bin yet — first launch, route to onboarding. */
    data object Onboarding : AppState

    /** auth_meta.bin exists but no unlocked DB. Show lock screen. */
    data object Locked : AppState

    /** Wrong-PIN cooldown. */
    data class LockedCooldown(val expiryEpochMs: Long) : AppState

    /** Decoy mode after duress PIN. */
    data class UnlockedDecoy(val db: TidesDatabase) : AppState

    /** Real unlocked state. */
    data class Unlocked(val db: TidesDatabase) : AppState
}
