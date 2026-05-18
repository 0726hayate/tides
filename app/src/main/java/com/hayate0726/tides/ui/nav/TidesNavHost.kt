package com.hayate0726.tides.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hayate0726.tides.AppState
import com.hayate0726.tides.AppViewModel
import com.hayate0726.tides.ui.lock.LockHost

/**
 * Root navigation host. Observes AppViewModel.state and routes between
 * Onboarding / Lock / Main destinations.
 *
 * While AppState is Loading we render a CircularProgressIndicator and
 * defer building NavHost so its `startDestination` can be chosen from
 * the real initial state. Without this, NavHost would briefly compose
 * the Onboarding subgraph on every launch (including launches where
 * auth_meta.bin already exists), creating an OnboardingViewModel and
 * running its init side-effects only to immediately pop it.
 */
@Composable
fun TidesNavHost() {
    val app: AppViewModel = hiltViewModel()
    val state by app.state.collectAsStateWithLifecycle()

    if (state is AppState.Loading) {
        LoadingScreen()
        return
    }

    val nav = rememberNavController()
    val startDestination = remember(/* state captured once on first non-Loading frame */) {
        when (state) {
            AppState.Onboarding -> Routes.Onboarding
            AppState.Locked, is AppState.LockedCooldown -> Routes.Lock
            is AppState.Unlocked, is AppState.UnlockedDecoy -> Routes.Main
            AppState.Loading -> Routes.Onboarding // unreachable; the if above returned
        }
    }

    // Subsequent state transitions: route between top-level destinations.
    // Keyed on the target route (not the full AppState) so Unlocked(db1) ->
    // Unlocked(db2) doesn't pop the back stack just because the db instance
    // swapped.
    val targetRoute = when (state) {
        AppState.Onboarding -> Routes.Onboarding
        AppState.Locked, is AppState.LockedCooldown -> Routes.Lock
        is AppState.Unlocked, is AppState.UnlockedDecoy -> Routes.Main
        AppState.Loading -> null
    }
    LaunchedEffect(targetRoute) {
        if (targetRoute == null) return@LaunchedEffect
        if (nav.currentBackStackEntry?.destination?.route == targetRoute) return@LaunchedEffect
        // Don't navigate on first composition — startDestination already points there.
        if (nav.currentBackStackEntry == null) return@LaunchedEffect
        nav.navigate(targetRoute) { popUpTo(0) { inclusive = true } }
    }

    NavHost(navController = nav, startDestination = startDestination) {
        onboardingNavGraph(nav, onComplete = { db -> app.setUnlocked(db) })

        composable(Routes.Lock) {
            LockHost(appViewModel = app)
        }

        composable(Routes.Main) {
            MainHost(appViewModel = app)
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
