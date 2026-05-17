package com.hayate0726.tides.domain.model

enum class Goal {
    TRACK_PERIOD,
    TRACK_SYMPTOMS,
    MANAGE_CONDITION,
    AVOID_PREGNANCY,
    TRYING_TO_CONCEIVE,
    JUST_CURIOUS;

    companion object {
        /**
         * Per spec §5.1, ovulation/fertile-window UI is suppressed unless
         * the user has at least one of these goals selected.
         */
        val OVULATION_RELEVANT: Set<Goal> = setOf(AVOID_PREGNANCY, TRYING_TO_CONCEIVE)
    }
}
