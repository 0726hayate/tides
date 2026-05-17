package com.hayate0726.tides.lock

import com.hayate0726.tides.crypto.AuthMeta
import com.hayate0726.tides.crypto.DbKey
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin

/**
 * Stateless logic for PIN attempts. The UI layer feeds in PIN entries
 * and gets back UnlockResult. Rate-limit and cooldown are enforced here.
 *
 * Lock state (the StateFlow) lives in a ViewModel in Plan 3.
 */
class LockManager(
    private val store: AuthMetaStore,
    private val clock: Clock,
) {

    fun interface Clock {
        fun nowMs(): Long
    }

    interface AuthMetaStore {
        fun load(): AuthMeta
        fun update(updater: (AuthMeta) -> AuthMeta)
    }

    sealed interface UnlockResult {
        data class Success(val key: DbKey) : UnlockResult
        data object WrongPin : UnlockResult
        data object Duress : UnlockResult
        data class RateLimited(val expiryEpochMs: Long) : UnlockResult
    }

    /**
     * Try to unlock with the given PIN. Caller is responsible for zeroing
     * the PIN after this call returns.
     */
    fun attemptUnlock(pin: Pin): UnlockResult {
        val meta = store.load()
        val now = clock.nowMs()

        if (meta.cooldownExpiryEpochMs > now) {
            return UnlockResult.RateLimited(meta.cooldownExpiryEpochMs)
        }

        if (KeyDerivation.validatePin(pin, meta.pinHashSalt, meta.pinHash)) {
            val key = KeyDerivation.deriveKey(pin, meta.keySalt)
            store.update { it.copyWith(failCount = 0, cooldownExpiryEpochMs = 0L) }
            return UnlockResult.Success(key)
        }

        val duress = meta.duress
        if (duress != null &&
            KeyDerivation.validatePin(pin, duress.pinHashSalt, duress.pinHash)
        ) {
            return UnlockResult.Duress
        }

        val newFailCount = meta.failCount + 1
        val newCooldownExpiry = if (newFailCount % FAIL_BATCH_SIZE == 0) {
            val batchNumber = newFailCount / FAIL_BATCH_SIZE
            now + computeCooldownMs(batchNumber)
        } else {
            meta.cooldownExpiryEpochMs
        }
        store.update {
            it.copyWith(failCount = newFailCount, cooldownExpiryEpochMs = newCooldownExpiry)
        }
        return UnlockResult.WrongPin
    }

    private fun computeCooldownMs(batchNumber: Int): Long {
        val base = 30_000L
        val shift = (batchNumber - 1).coerceAtLeast(0).coerceAtMost(20)
        return (base shl shift).coerceAtMost(MAX_COOLDOWN_MS)
    }

    companion object {
        const val FAIL_BATCH_SIZE = 5
        const val MAX_COOLDOWN_MS = 3_600_000L
    }
}

internal fun AuthMeta.copyWith(
    failCount: Int = this.failCount,
    cooldownExpiryEpochMs: Long = this.cooldownExpiryEpochMs,
): AuthMeta = AuthMeta(
    keySalt = this.keySalt,
    pinHashSalt = this.pinHashSalt,
    pinHash = this.pinHash,
    duress = this.duress,
    failCount = failCount,
    cooldownExpiryEpochMs = cooldownExpiryEpochMs,
)
