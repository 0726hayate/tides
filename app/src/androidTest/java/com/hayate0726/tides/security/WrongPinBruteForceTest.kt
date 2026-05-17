package com.hayate0726.tides.security

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hayate0726.tides.crypto.AuthMeta
import com.hayate0726.tides.crypto.KeyDerivation
import com.hayate0726.tides.crypto.Pin
import com.hayate0726.tides.lock.LockManager
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

/**
 * Throws 200 random wrong PINs at LockManager and verifies none of them
 * are reported as Success or Duress. The cooldown/failCount fields are
 * reset between attempts so we're testing the validatePin invariant in
 * isolation — Task 14 covers rate-limit behavior.
 *
 * 200 iterations (rather than the originally-spec'd 1000) keeps the
 * total Argon2 cost on the emulator under ~60s. Adjust upward if we
 * ever decide to weaken Argon2 params.
 */
@RunWith(AndroidJUnit4::class)
class WrongPinBruteForceTest {

    @Test
    fun no_wrong_pin_unlocks() {
        val pinHashSalt = ByteArray(16) { 9 }
        val correctPin = "847291"
        val correctHash = KeyDerivation.derivePinHash(Pin(correctPin.toCharArray()), pinHashSalt)
        val baseMeta = AuthMeta(
            keySalt = ByteArray(16) { 1 },
            pinHashSalt = pinHashSalt,
            pinHash = correctHash,
            duress = null,
            failCount = 0,
            cooldownExpiryEpochMs = 0L,
        )

        val store = object : LockManager.AuthMetaStore {
            private var state = baseMeta
            override fun load() = state
            override fun update(updater: (AuthMeta) -> AuthMeta) { state = updater(state) }
        }
        val mgr = LockManager(store, LockManager.Clock { 0L })

        val rng = Random(seed = 42)
        repeat(200) {
            var candidate = rng.nextInt(0, 1_000_000).toString().padStart(6, '0')
            if (candidate == correctPin) candidate = "000000"
            store.update {
                AuthMeta(it.keySalt, it.pinHashSalt, it.pinHash, it.duress, 0, 0L)
            }
            val r = mgr.attemptUnlock(Pin(candidate.toCharArray()))
            assertTrue(
                "Wrong PIN '$candidate' was reported as $r",
                r !is LockManager.UnlockResult.Success && r !is LockManager.UnlockResult.Duress
            )
        }
    }
}
