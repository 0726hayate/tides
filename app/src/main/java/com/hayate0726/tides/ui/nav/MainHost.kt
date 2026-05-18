package com.hayate0726.tides.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hayate0726.tides.AppState
import com.hayate0726.tides.AppViewModel

/**
 * Main route host. Pulls the unlocked TidesDatabase off AppState and hands
 * off to [MainScaffold], which renders Calendar / Stats / Settings with a
 * bottom navigation bar and the Settings sub-routes (Notifications, Backup,
 * Duress, ThreatPreset, Feedback).
 *
 * If AppState drops out of unlocked (the moment after lock() but before the
 * NavHost routes us elsewhere), we just render nothing — the parent's
 * LaunchedEffect on targetRoute will swap us out shortly.
 */
@Composable
fun MainHost(appViewModel: AppViewModel) {
    val state by appViewModel.state.collectAsStateWithLifecycle()
    val db = when (val s = state) {
        is AppState.Unlocked -> s.db
        is AppState.UnlockedDecoy -> s.db
        else -> null
    } ?: return

    CompositionLocalProvider(LocalTidesDatabase provides db) {
        MainScaffold(appViewModel = appViewModel, db = db)
    }
}
