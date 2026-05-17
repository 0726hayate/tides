package com.hayate0726.tides.lock

import com.hayate0726.tides.crypto.DbKey

sealed interface LockState {
    data object Locked : LockState
    data object UnlockingPin : LockState
    data object UnlockingBiometric : LockState
    data class Unlocked(val key: DbKey) : LockState
    data class LockedCooldown(val expiryEpochMs: Long) : LockState
    data object DuressDecoy : LockState
    data object DuressWipe : LockState
}
