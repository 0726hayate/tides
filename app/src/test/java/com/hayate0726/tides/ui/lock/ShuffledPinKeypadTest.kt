package com.hayate0726.tides.ui.lock

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ShuffledPinKeypadTest {

    @Test
    fun `shuffledOrder contains digits 1 through 9 exactly once`() {
        repeat(50) {
            val order = shuffledDigitOrder()
            assertEquals(9, order.size, "must be 9 digits")
            assertEquals((1..9).toSet(), order.toSet(), "must contain 1-9 exactly once")
        }
    }

    @Test
    fun `shuffledOrder is not always canonical`() {
        val anyDifferent = (1..50).any { shuffledDigitOrder() != listOf(1,2,3,4,5,6,7,8,9) }
        assertTrue(anyDifferent, "50 shuffles should not all be canonical order")
    }
}
