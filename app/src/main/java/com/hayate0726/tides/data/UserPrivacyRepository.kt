package com.hayate0726.tides.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single source of truth for [UserPrivacyView] while the app is unlocked.
 * Calendar, Stats, and the Normal widget all read from this rather than
 * each going back to the DAO.
 *
 * The repository is filled by callers (CalendarViewModel and the
 * BirthControlViewModel) calling [refresh]. Not Hilt-injected as a
 * singleton-scoped DB consumer because the DB instance changes on each
 * unlock — callers pass the active DB at refresh time.
 *
 * We avoid an explicit DB reference in the singleton to dodge the same
 * lifecycle hazard CalendarViewModel solves via System.identityHashCode
 * keying.
 */
class UserPrivacyRepository {
    private val _view = MutableStateFlow(UserPrivacyView(showOvulation = false))
    val view: StateFlow<UserPrivacyView> = _view.asStateFlow()

    suspend fun refresh(db: TidesDatabase) {
        val goals = db.goalDao().all().toSet()
        val bc = db.birthControlDao().activeOnce()?.method
        _view.value = UserPrivacyView.compute(goals, bc)
    }
}
