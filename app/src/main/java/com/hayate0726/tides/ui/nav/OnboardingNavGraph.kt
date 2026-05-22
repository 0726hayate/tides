package com.hayate0726.tides.ui.nav

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.hayate0726.tides.data.TidesDatabase
import com.hayate0726.tides.ui.onboarding.BiometricSetupScreen
import com.hayate0726.tides.ui.onboarding.GoalsScreen
import com.hayate0726.tides.ui.onboarding.FlowSymptomsScreen
import com.hayate0726.tides.ui.onboarding.ImportPromptScreen
import com.hayate0726.tides.ui.onboarding.LastPeriodScreen
import com.hayate0726.tides.ui.onboarding.OnboardingBirthControlScreen
import com.hayate0726.tides.ui.onboarding.PredictionPreviewScreen
import com.hayate0726.tides.ui.onboarding.PredictionPreviewViewModel
import com.hayate0726.tides.ui.onboarding.OnboardingComplete
import com.hayate0726.tides.ui.onboarding.OnboardingStep
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
            val vm = sharedOnboardingVm(nav) ?: return@composable
            val step by vm.currentStep.collectAsStateWithLifecycle()
            val hasDraft = remember(step) { vm.draftStore.exists() }
            WelcomeScreen(
                hasDraft = hasDraft,
                onResume = {
                    vm.resumeFromDraft()
                    val targetRoute = when (vm.currentStep.value) {
                        OnboardingStep.GOALS -> Routes.Goals
                        OnboardingStep.PIN -> Routes.PinSetup
                        OnboardingStep.BIOMETRIC -> Routes.BiometricSetup
                        OnboardingStep.THREAT -> Routes.ThreatPreset
                        OnboardingStep.BIRTH_CONTROL -> Routes.OnboardingBirthControl
                        OnboardingStep.IMPORT_PROMPT -> Routes.OnboardingImportPrompt
                        OnboardingStep.LAST_PERIOD -> Routes.LastPeriod
                        OnboardingStep.FLOW_SYMPTOMS -> Routes.FlowSymptoms
                        OnboardingStep.PREDICTION -> Routes.Prediction
                        OnboardingStep.WELCOME,
                        OnboardingStep.COMPLETE -> Routes.Goals
                    }
                    nav.navigate(targetRoute)
                },
                onStartOver = { vm.startFresh() },
                onContinue = { nav.navigate(Routes.Goals) },
            )
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
                    nav.navigate(Routes.OnboardingBirthControl)
                },
            )
        }
        composable(Routes.OnboardingBirthControl) {
            val vm = sharedOnboardingVm(nav) ?: return@composable
            val draft by vm.draft.collectAsStateWithLifecycle()
            OnboardingBirthControlScreen(
                initial = draft.birthControl,
                onContinue = {
                    vm.setBc(it)
                    nav.navigate(Routes.OnboardingImportPrompt)
                },
            )
        }
        composable(Routes.OnboardingImportPrompt) {
            sharedOnboardingVm(nav) ?: return@composable
            ImportPromptScreen(
                onImport = { nav.navigate(Routes.Import) },
                onStartFresh = { nav.navigate(Routes.LastPeriod) },
            )
        }
        composable(Routes.LastPeriod) {
            val vm = sharedOnboardingVm(nav) ?: return@composable
            val wasImported by vm.wasImported.collectAsStateWithLifecycle()
            if (wasImported) {
                LaunchedEffect(Unit) {
                    vm.complete()
                    nav.navigate(Routes.OnboardingCompleteRoute)
                }
                return@composable
            }
            LastPeriodScreen(onFinish = { date ->
                vm.setLastPeriodStart(date)
                if (date == null) {
                    vm.complete()
                    nav.navigate(Routes.OnboardingCompleteRoute)
                } else {
                    nav.navigate(Routes.FlowSymptoms)
                }
            })
        }
        composable(Routes.FlowSymptoms) {
            val vm = sharedOnboardingVm(nav) ?: return@composable
            FlowSymptomsScreen(
                onSave = { flow, symptoms ->
                    vm.setFlow(flow)
                    vm.setSymptoms(symptoms)
                    nav.navigate(Routes.Prediction)
                },
                onSkipRest = { flow, symptoms ->
                    vm.setFlow(flow)
                    vm.setSymptoms(symptoms)
                    nav.navigate(Routes.Prediction)
                },
            )
        }
        composable(Routes.Prediction) {
            val vm = sharedOnboardingVm(nav) ?: return@composable
            val draft by vm.draft.collectAsStateWithLifecycle()
            val lastPeriodStart = draft.lastPeriodStart
            if (lastPeriodStart == null) {
                LaunchedEffect(Unit) {
                    vm.complete()
                    nav.navigate(Routes.OnboardingCompleteRoute)
                }
                return@composable
            }
            val state = remember(lastPeriodStart, draft.goals, draft.birthControl) {
                PredictionPreviewViewModel.compute(
                    lastPeriodStart = lastPeriodStart,
                    goals = draft.goals,
                    birthControl = draft.birthControl,
                )
            }
            PredictionPreviewScreen(
                state = state,
                onEdit = {
                    nav.popBackStack(Routes.LastPeriod, inclusive = false)
                },
                onConfirm = {
                    vm.complete()
                    nav.navigate(Routes.OnboardingCompleteRoute)
                },
            )
        }
        composable(Routes.OnboardingCompleteRoute) {
            // Resolve the shared VM exactly once, on first composition, and
            // capture its `completion` flow in a remember. After onComplete(db)
            // fires, AppViewModel.setUnlocked pops the onboarding subgraph;
            // we must NOT re-resolve the VM (would throw — Routes.Onboarding
            // no longer exists on the back stack).
            val initialVm = sharedOnboardingVm(nav)
            val completion = remember { initialVm?.completion }
            LaunchedEffect(completion) {
                val db = completion?.filterNotNull()?.first() ?: return@LaunchedEffect
                onComplete(db)
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
