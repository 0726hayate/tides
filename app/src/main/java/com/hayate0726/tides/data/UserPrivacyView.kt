package com.hayate0726.tides.data

import com.hayate0726.tides.domain.model.BirthControlMethod
import com.hayate0726.tides.domain.model.Goal

/**
 * UI-suppression flags derived from active birth control method.
 *
 * As of v1.3 the goal-based gate is removed — ovulation/fertile-window UI
 * is shown for everyone whose active BC method is non-hormonal (or unknown).
 * The `goals` parameter is retained on the signature for now to avoid call-
 * site churn, but is ignored. Phase predictions are still suppressed when
 * the active method is hormonal because the fixed-luteal heuristic doesn't
 * apply under ovulation suppression.
 */
data class UserPrivacyView(val showOvulation: Boolean) {
    companion object {
        fun compute(goals: Set<Goal>, activeBc: BirthControlMethod?): UserPrivacyView {
            val bcAllows = activeBc?.isHormonal != true
            return UserPrivacyView(showOvulation = bcAllows)
        }
    }
}
