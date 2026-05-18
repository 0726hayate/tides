package com.hayate0726.tides.data

import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.domain.model.Goal

/**
 * UI-suppression flags derived from goals + active birth control method.
 * Spec §5.1: ovulation/fertile-window UI is suppressed unless the user
 * has at least one OVULATION_RELEVANT goal, AND the active BC method is
 * non-hormonal (or unknown).
 */
data class UserPrivacyView(val showOvulation: Boolean) {
    companion object {
        fun compute(goals: Set<Goal>, activeBc: BirthControlMethod?): UserPrivacyView {
            val goalRelevant = goals.any { it in Goal.OVULATION_RELEVANT }
            val bcAllows = activeBc?.isHormonal != true
            return UserPrivacyView(showOvulation = goalRelevant && bcAllows)
        }
    }
}
