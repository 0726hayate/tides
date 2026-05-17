package com.hayate0726.tides.domain.model

enum class FlowIntensity(val intCode: Int) {
    NONE(0),
    SPOTTING(1),
    LIGHT(2),
    MEDIUM(3),
    HEAVY(4);

    companion object {
        fun fromInt(value: Int): FlowIntensity =
            entries.first { it.intCode == value }

        /** Per FIGO, "heavy" includes our HEAVY only (spec §5.7). */
        fun isHeavy(intensity: FlowIntensity) = intensity == HEAVY

        /** Any actual bleeding, excluding NONE. Used for cycle detection. */
        fun isBleeding(intensity: FlowIntensity) = intensity != NONE
    }
}
