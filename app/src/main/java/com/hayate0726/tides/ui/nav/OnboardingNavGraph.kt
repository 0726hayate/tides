package com.hayate0726.tides.ui.nav

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.hayate0726.tides.ui.onboarding.BiometricSetupScreen
import com.hayate0726.tides.ui.onboarding.GoalsScreen
import com.hayate0726.tides.ui.onboarding.LastPeriodScreen
import com.hayate0726.tides.ui.onboarding.OnboardingViewModel
import com.hayate0726.tides.ui.onboarding.PinSetupScreen
import com.hayate0726.tides.ui.onboarding.ThreatPresetScreen
import com.hayate0726.tides.ui.onboarding.WelcomeScreen

fun NavGraphBuilder.onboardingNavGraph(
    nav: NavHostController,
    onComplete: () -> Unit,
) {
    navigation(startDestination = Routes.Welcome, route = Routes.Onboarding) {
        composable(Routes.Welcome) {
            WelcomeScreen(onContinue = { nav.navigate(Routes.Goals) })
        }
        composable(Routes.Goals) {
            val vm: OnboardingViewModel = sharedOnboardingVm(nav.getBackStackEntry(Routes.Onboarding))
            val draft by vm.draft.collectAsState()
            GoalsScreen(
                initialGoals = draft.goals,
                onContinue = {
                    vm.setGoals(it)
                    nav.navigate(Routes.PinSetup)
                },
            )
        }
        composable(Routes.PinSetup) {
            val vm: OnboardingViewModel = sharedOnboardingVm(nav.getBackStackEntry(Routes.Onboarding))
            PinSetupScreen(onContinue = {
                vm.setPin(it)
                nav.navigate(Routes.BiometricSetup)
            })
        }
        composable(Routes.BiometricSetup) {
            val vm: OnboardingViewModel = sharedOnboardingVm(nav.getBackStackEntry(Routes.Onboarding))
            val draft by vm.draft.collectAsState()
            BiometricSetupScreen(
                initialEnabled = draft.biometricEnabled,
                onContinue = {
                    vm.setBiometric(it)
                    nav.navigate(Routes.ThreatPreset)
                },
            )
        }
        composable(Routes.ThreatPreset) {
            val vm: OnboardingViewModel = sharedOnboardingVm(nav.getBackStackEntry(Routes.Onboarding))
            val draft by vm.draft.collectAsState()
            ThreatPresetScreen(
                initial = draft.threatPreset,
                onContinue = {
                    vm.setThreatPreset(it)
                    nav.navigate(Routes.LastPeriod)
                },
            )
        }
        composable(Routes.LastPeriod) {
            val vm: OnboardingViewModel = sharedOnboardingVm(nav.getBackStackEntry(Routes.Onboarding))
            LastPeriodScreen(onFinish = {
                vm.setLastPeriodStart(it)
                vm.complete()
                onComplete()
            })
        }
    }
}

// Helper to share the OnboardingViewModel across the nav graph by scoping it to
// the parent NavBackStackEntry (the "onboarding" route).
@androidx.compose.runtime.Composable
private fun sharedOnboardingVm(parentEntry: androidx.navigation.NavBackStackEntry): OnboardingViewModel {
    return androidx.hilt.navigation.compose.hiltViewModel(viewModelStoreOwner = parentEntry)
}
