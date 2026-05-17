package com.hayate0726.tides.ui.nav

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.hayate0726.tides.data.TidesDatabase
import com.hayate0726.tides.ui.onboarding.BiometricSetupScreen
import com.hayate0726.tides.ui.onboarding.GoalsScreen
import com.hayate0726.tides.ui.onboarding.LastPeriodScreen
import com.hayate0726.tides.ui.onboarding.OnboardingComplete
import com.hayate0726.tides.ui.onboarding.OnboardingViewModel
import com.hayate0726.tides.ui.onboarding.PinSetupScreen
import com.hayate0726.tides.ui.onboarding.ThreatPresetScreen
import com.hayate0726.tides.ui.onboarding.WelcomeScreen

fun NavGraphBuilder.onboardingNavGraph(
    nav: NavHostController,
    onComplete: (TidesDatabase) -> Unit,
) {
    navigation(startDestination = Routes.Welcome, route = Routes.Onboarding) {
        composable(Routes.Welcome) {
            WelcomeScreen(onContinue = { nav.navigate(Routes.Goals) })
        }
        composable(Routes.Goals) {
            val vm = sharedOnboardingVm(nav) ?: return@composable
            val draft by vm.draft.collectAsStateWithLifecycle()
            GoalsScreen(
                initialGoals = draft.goals,
                onContinue = {
                    vm.setGoals(it)
                    nav.navigate(Routes.PinSetup)
                },
            )
        }
        composable(Routes.PinSetup) {
            val vm = sharedOnboardingVm(nav) ?: return@composable
            PinSetupScreen(onContinue = {
                vm.setPin(it)
                nav.navigate(Routes.BiometricSetup)
            })
        }
        composable(Routes.BiometricSetup) {
            val vm = sharedOnboardingVm(nav) ?: return@composable
            val draft by vm.draft.collectAsStateWithLifecycle()
            BiometricSetupScreen(
                initialEnabled = draft.biometricEnabled,
                onContinue = {
                    vm.setBiometric(it)
                    nav.navigate(Routes.ThreatPreset)
                },
            )
        }
        composable(Routes.ThreatPreset) {
            val vm = sharedOnboardingVm(nav) ?: return@composable
            val draft by vm.draft.collectAsStateWithLifecycle()
            ThreatPresetScreen(
                initial = draft.threatPreset,
                onContinue = {
                    vm.setThreatPreset(it)
                    nav.navigate(Routes.LastPeriod)
                },
            )
        }
        composable(Routes.LastPeriod) {
            val vm = sharedOnboardingVm(nav) ?: return@composable
            LastPeriodScreen(onFinish = {
                vm.setLastPeriodStart(it)
                vm.complete()
                nav.navigate(Routes.OnboardingCompleteRoute)
            })
        }
        composable(Routes.OnboardingCompleteRoute) {
            val vm = sharedOnboardingVm(nav)
            val db = vm?.completion?.collectAsStateWithLifecycle()?.value
            LaunchedEffect(db) {
                db?.let { onComplete(it) }
            }
            OnboardingComplete()
        }
    }
}

/**
 * Share the OnboardingViewModel across the nav graph by scoping it to the
 * parent NavBackStackEntry (the "onboarding" route).
 *
 * Returns null while the onboarding subgraph is being popped (the moment
 * after setUnlocked but before MainHost replaces the composition). Callers
 * must handle null gracefully — the screen is about to be removed anyway.
 */
@androidx.compose.runtime.Composable
private fun sharedOnboardingVm(nav: NavHostController): OnboardingViewModel? {
    val parentEntry = runCatching { nav.getBackStackEntry(Routes.Onboarding) }
        .getOrNull() ?: return null
    return androidx.hilt.navigation.compose.hiltViewModel(viewModelStoreOwner = parentEntry)
}
