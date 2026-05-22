package com.hayate0726.tides.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hayate0726.tides.AppState
import com.hayate0726.tides.AppViewModel
import com.hayate0726.tides.ui.dataimport.ImportPreviewScreen
import com.hayate0726.tides.ui.dataimport.ImportResultScreen
import com.hayate0726.tides.ui.dataimport.ImportScreen
import com.hayate0726.tides.ui.dataimport.ImportViewModel
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
            MainHost(appViewModel = app, rootNav = nav)
        }

        composable(Routes.Import) {
            RootImportRoute(app, nav)
        }

        composable(Routes.ImportPreview) {
            RootImportPreviewRoute(app, nav)
        }

        composable(Routes.ImportResult) {
            RootImportResultRoute(app, nav)
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

// ---------------------------------------------------------------------------
// Root-level Import composables
// Reachable from both the Onboarding subgraph (Welcome → Import) and the
// Main subgraph (Settings → Import). Context is detected at runtime by
// checking whether Routes.Onboarding is present on the root back stack.
// ---------------------------------------------------------------------------

@Composable
private fun RootImportRoute(appViewModel: AppViewModel, nav: NavHostController) {
    val vm: ImportViewModel = hiltViewModel()
    val status by vm.status.collectAsStateWithLifecycle()

    // Detect context: are we in onboarding or main?
    val isOnboarding = remember(nav.currentBackStackEntry) {
        runCatching { nav.getBackStackEntry(Routes.Onboarding) }.getOrNull() != null
    }

    // For the Settings path we need the unlocked DB.
    val appState by appViewModel.state.collectAsStateWithLifecycle()
    val db = (appState as? AppState.Unlocked)?.db
        ?: (appState as? AppState.UnlockedDecoy)?.db

    RootSubScreenScaffold("Import from another app", nav) { p ->
        Box(modifier = Modifier.padding(p)) {
            ImportScreen(
                vm = vm,
                onPick = { uri ->
                    if (isOnboarding) vm.pickedFileForOnboarding(uri)
                    else if (db != null) vm.pickedFile(uri, db)
                },
            )
        }
    }
    LaunchedEffect(status) {
        if (status is ImportViewModel.Status.PreviewReady) {
            nav.navigate(Routes.ImportPreview)
        }
    }
}

@Composable
private fun RootImportPreviewRoute(appViewModel: AppViewModel, nav: NavHostController) {
    val parentEntry = remember(nav.currentBackStackEntry) {
        runCatching { nav.getBackStackEntry(Routes.Import) }.getOrNull()
    }
    if (parentEntry == null) {
        LaunchedEffect(Unit) { nav.popBackStack() }
        return
    }
    val vm: ImportViewModel = hiltViewModel(viewModelStoreOwner = parentEntry)
    val status by vm.status.collectAsStateWithLifecycle()
    val preview = (status as? ImportViewModel.Status.PreviewReady)?.preview
        ?: (status as? ImportViewModel.Status.Committing)?.preview
    if (preview == null) {
        LaunchedEffect(Unit) { nav.popBackStack() }
        return
    }

    // Detect context
    val isOnboarding = remember(nav.currentBackStackEntry) {
        runCatching { nav.getBackStackEntry(Routes.Onboarding) }.getOrNull() != null
    }

    val onboardingEntry = if (isOnboarding) {
        remember { runCatching { nav.getBackStackEntry(Routes.Onboarding) }.getOrNull() }
    } else null
    val onboardingVm: com.hayate0726.tides.ui.onboarding.OnboardingViewModel? =
        if (onboardingEntry != null) hiltViewModel(viewModelStoreOwner = onboardingEntry) else null

    val appState by appViewModel.state.collectAsStateWithLifecycle()
    val db = (appState as? AppState.Unlocked)?.db
        ?: (appState as? AppState.UnlockedDecoy)?.db

    RootSubScreenScaffold("Import preview", nav) { p ->
        Box(modifier = Modifier.padding(p)) {
            ImportPreviewScreen(
                preview = preview,
                onCancel = {
                    vm.reset()
                    nav.popBackStack(Routes.Import, inclusive = false)
                },
                onConfirm = {
                    if (isOnboarding && onboardingVm != null) {
                        vm.confirmImportForOnboarding(onboardingVm) {
                            nav.navigate(Routes.ImportResult)
                        }
                    } else if (db != null) {
                        vm.confirmImport(db) {
                            nav.navigate(Routes.ImportResult)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun RootImportResultRoute(appViewModel: AppViewModel, nav: NavHostController) {
    val parentEntry = remember(nav.currentBackStackEntry) {
        runCatching { nav.getBackStackEntry(Routes.Import) }.getOrNull()
    }
    if (parentEntry == null) {
        LaunchedEffect(Unit) { nav.popBackStack() }
        return
    }
    val vm: ImportViewModel = hiltViewModel(viewModelStoreOwner = parentEntry)
    val status by vm.status.collectAsStateWithLifecycle()
    val result = (status as? ImportViewModel.Status.Complete)?.result
    if (result == null) {
        LaunchedEffect(Unit) { nav.popBackStack() }
        return
    }

    val onboardingEntry = remember {
        runCatching { nav.getBackStackEntry(Routes.Onboarding) }.getOrNull()
    }
    val onboardingVm: com.hayate0726.tides.ui.onboarding.OnboardingViewModel? =
        if (onboardingEntry != null) hiltViewModel(viewModelStoreOwner = onboardingEntry) else null

    RootSubScreenScaffold("Import complete", nav) { p ->
        Box(modifier = Modifier.padding(p)) {
            ImportResultScreen(
                result = result,
                onDone = {
                    if (onboardingVm != null) {
                        onboardingVm.markImported()
                        nav.navigate(Routes.PinSetup) {
                            popUpTo(Routes.Welcome) { inclusive = false }
                        }
                    } else {
                        appViewModel.refreshPreImportSnapshotState()
                        nav.popBackStack(Routes.Settings, inclusive = false)
                    }
                    vm.reset()
                },
            )
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun RootSubScreenScaffold(
    title: String,
    nav: NavHostController,
    content: @Composable (PaddingValues) -> Unit,
) {
    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { androidx.compose.material3.Text(title) },
                navigationIcon = {
                    androidx.compose.material3.IconButton(onClick = { nav.popBackStack() }) {
                        androidx.compose.material3.Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { inner -> content(inner) }
}
