package com.hayate0726.tides.ui.nav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hayate0726.tides.AppState
import com.hayate0726.tides.AppViewModel

/**
 * Root navigation host. Observes AppViewModel.state and routes between
 * Loading / Onboarding / Lock / Main destinations.
 *
 * Plan 3 ships a minimal Main route (placeholder) — the full Calendar /
 * Log / Stats / Settings integration is wired into Main in Plan 4 once
 * the unlocked TidesDatabase can be passed down through a CompositionLocal
 * or a feature-level ViewModel factory.
 */
@Composable
fun TidesNavHost() {
    val app: AppViewModel = hiltViewModel()
    val state by app.state.collectAsStateWithLifecycle()
    val nav = rememberNavController()

    LaunchedEffect(state) {
        val target = when (state) {
            AppState.Loading -> return@LaunchedEffect
            AppState.Onboarding -> Routes.Onboarding
            AppState.Locked, is AppState.LockedCooldown -> Routes.Lock
            is AppState.Unlocked, is AppState.UnlockedDecoy -> Routes.Main
        }
        if (nav.currentBackStackEntry?.destination?.route != target) {
            nav.navigate(target) { popUpTo(0) { inclusive = true } }
        }
    }

    if (state is AppState.Loading) {
        LoadingScreen()
        return
    }

    NavHost(navController = nav, startDestination = Routes.Onboarding) {
        onboardingNavGraph(nav, onComplete = { db -> app.setUnlocked(db) })

        composable(Routes.Lock) {
            // Minimal lock placeholder. Full LockScreen integration with
            // LockManager + BiometricPrompt happens next.
            PlaceholderScreen(
                title = "Locked",
                subtitle = "Lock screen wiring lands next.",
            )
        }

        composable(Routes.Main) {
            PlaceholderScreen(
                title = "Tides",
                subtitle = "Calendar / Log / Stats are built; nav-host wiring of the unlocked DB lands next.",
                actionLabel = "Lock",
                onAction = { app.lock() },
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}
