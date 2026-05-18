@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hayate0726.tides.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hayate0726.tides.AppState
import com.hayate0726.tides.AppViewModel
import com.hayate0726.tides.data.TidesDatabase
import com.hayate0726.tides.domain.model.ThreatPreset
import com.hayate0726.tides.ui.calendar.CalendarMonthState
import com.hayate0726.tides.ui.calendar.CalendarScreen
import com.hayate0726.tides.ui.calendar.CalendarViewModel
import com.hayate0726.tides.ui.feedback.FeedbackScreen
import com.hayate0726.tides.ui.log.LogBottomSheet
import com.hayate0726.tides.ui.log.LogViewModel
import com.hayate0726.tides.ui.onboarding.ThreatPresetScreen
import com.hayate0726.tides.ui.settings.BackupScreen
import com.hayate0726.tides.ui.settings.BackupViewModel
import com.hayate0726.tides.ui.settings.DuressSetupScreen
import com.hayate0726.tides.ui.settings.DuressSetupViewModel
import com.hayate0726.tides.ui.settings.NotificationsScreen
import com.hayate0726.tides.ui.settings.NotificationsViewModel
import com.hayate0726.tides.ui.settings.SettingsScreen
import com.hayate0726.tides.ui.stats.StatsScreen
import com.hayate0726.tides.ui.stats.StatsViewModel
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hayate0726.tides.data.entity.SettingsEntity
import java.time.temporal.ChronoUnit

@Composable
fun MainScaffold(appViewModel: AppViewModel, db: TidesDatabase) {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val route = current?.destination?.route
    val showBottomBar = route in setOf(Routes.Calendar, Routes.Stats, Routes.Settings)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = route == Routes.Calendar,
                        onClick = { navigateTab(nav, Routes.Calendar) },
                        icon = {},
                        label = { Text("Calendar") },
                    )
                    NavigationBarItem(
                        selected = route == Routes.Stats,
                        onClick = { navigateTab(nav, Routes.Stats) },
                        icon = {},
                        label = { Text("Stats") },
                    )
                    NavigationBarItem(
                        selected = route == Routes.Settings,
                        onClick = { navigateTab(nav, Routes.Settings) },
                        icon = {},
                        label = { Text("Settings") },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = Routes.Calendar,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.Calendar) { CalendarRoute(db) }
            composable(Routes.Stats) { StatsRoute(db) }
            composable(Routes.Settings) { SettingsRoute(appViewModel, db, nav) }
            composable(Routes.SettingsNotifications) { NotificationsRoute(db) }
            composable(Routes.SettingsBackup) { BackupRoute() }
            composable(Routes.SettingsDuress) { DuressRoute(nav) }
            composable(Routes.SettingsThreatPreset) { ThreatPresetRoute(db, nav) }
            composable(Routes.SettingsFeedback) { FeedbackScreen() }
        }
    }
}

private fun navigateTab(nav: NavHostController, route: String) {
    nav.navigate(route) {
        popUpTo(Routes.Calendar) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun CalendarRoute(db: TidesDatabase) {
    val vm: CalendarViewModel = viewModel(
        key = "calendar-${System.identityHashCode(db)}",
        factory = simpleFactory { CalendarViewModel(db) },
    )
    val logVm: LogViewModel = viewModel(
        key = "log-${System.identityHashCode(db)}",
        factory = simpleFactory { LogViewModel(db) },
    )
    val ui by vm.state.collectAsStateWithLifecycle()
    val logState by logVm.state.collectAsStateWithLifecycle()
    var pendingLogDate by remember { mutableStateOf<java.time.LocalDate?>(null) }

    val periodDays = remember(ui.cycles) {
        ui.cycles.flatMap { c ->
            val end = c.periodEnd ?: c.start
            val days = ChronoUnit.DAYS.between(c.start, end).toInt()
            (0..days).map { c.start.plusDays(it.toLong()) }
        }.toSet()
    }
    CalendarScreen(
        monthState = CalendarMonthState(
            month = ui.month,
            today = ui.today,
            periodDays = periodDays,
            ovulationWindow = null,
            predictedPeriod = null,
            symptomDays = ui.symptomDays,
        ),
        view = ui.view,
        onViewChange = vm::changeView,
        onDayClick = {
            pendingLogDate = it
            logVm.load(it)
        },
    )

    val openDate = pendingLogDate
    if (openDate != null && logState.loaded && logState.date == openDate) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { pendingLogDate = null },
        ) {
            LogBottomSheet(
                date = openDate,
                cycleDay = cycleDayFor(openDate, ui.cycles),
                initialFlow = logState.flow,
                initialSymptoms = logState.symptoms,
                initialNote = logState.note,
                onSave = { flow, symptoms, note ->
                    logVm.save(
                        date = openDate,
                        flow = flow,
                        symptoms = symptoms,
                        painSeverity = logState.painSeverity,
                        note = note,
                        otherText = logState.otherText,
                    )
                    pendingLogDate = null
                    vm.refresh()
                },
                onCancel = { pendingLogDate = null },
            )
        }
    }
}

private fun cycleDayFor(date: java.time.LocalDate, cycles: List<com.hayate0726.tides.domain.model.Cycle>): Int? {
    val containing = cycles.firstOrNull { c ->
        val end = c.nextStart?.minusDays(1) ?: date
        !date.isBefore(c.start) && !date.isAfter(end)
    } ?: return null
    return ChronoUnit.DAYS.between(containing.start, date).toInt() + 1
}

@Composable
private fun StatsRoute(db: TidesDatabase) {
    val vm: StatsViewModel = viewModel(
        key = "stats-${System.identityHashCode(db)}",
        factory = simpleFactory { StatsViewModel(db) },
    )
    val ui by vm.state.collectAsStateWithLifecycle()
    val state = ui
    if (state == null) {
        Text("Loading…", modifier = Modifier.padding(24.dp))
        return
    }
    StatsScreen(
        state = state,
        onDismissInsight = vm::dismissInsight,
        onExportPdf = { /* PDF share wiring lands with bottom-bar action */ },
        onExportCsv = { /* CSV share wiring lands with bottom-bar action */ },
    )
}

@Composable
private fun SettingsRoute(appViewModel: AppViewModel, db: TidesDatabase, nav: NavHostController) {
    val ctx = LocalContext.current
    val appState by appViewModel.state.collectAsStateWithLifecycle()

    var presetLabel by remember { mutableStateOf("Loading…") }
    var duressAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(db) {
        val name = withContext(Dispatchers.IO) { db.settingsDao().get("threat_preset") }
        val preset = runCatching { ThreatPreset.valueOf(name ?: "") }
            .getOrDefault(ThreatPreset.DEFAULT)
        presetLabel = preset.label()
        duressAvailable = preset.duressAvailable
    }

    SettingsScreen(
        threatPresetLabel = presetLabel,
        onChangePreset = { nav.navigate(Routes.SettingsThreatPreset) },
        onNotifications = { nav.navigate(Routes.SettingsNotifications) },
        onBackup = { nav.navigate(Routes.SettingsBackup) },
        onDuress = { nav.navigate(Routes.SettingsDuress) },
        duressAvailable = duressAvailable,
        onCheckUpdates = {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://github.com/hayate0726/tides/releases"),
            )
            ctx.startActivity(intent)
        },
        onSendFeedback = { nav.navigate(Routes.SettingsFeedback) },
        onSupportDevelopment = {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://ko-fi.com/hayate0726"),
            )
            ctx.startActivity(intent)
        },
        onLock = { appViewModel.lock() },
        appStateIsUnlocked = appState is AppState.Unlocked || appState is AppState.UnlockedDecoy,
    )
}

@Composable
private fun NotificationsRoute(db: TidesDatabase) {
    val ctx = LocalContext.current
    val ep = remember {
        EntryPointAccessors.fromApplication(ctx.applicationContext, MainGraphEntryPoint::class.java)
    }
    val vm: NotificationsViewModel = viewModel(
        key = "notifications-${System.identityHashCode(db)}",
        factory = simpleFactory {
            NotificationsViewModel(
                ctx = ctx.applicationContext,
                db = db,
                prefs = ep.notificationPreferences(),
                scheduler = ep.reminderScheduler(),
            )
        },
    )
    LaunchedEffect(Unit) { vm.refreshSystemPermission() }
    val s by vm.state.collectAsStateWithLifecycle()
    NotificationsScreen(
        periodPredictedEnabled = s.periodPredictedEnabled,
        periodStartEnabled = s.periodStartEnabled,
        latePeriodEnabled = s.latePeriodEnabled,
        systemNotificationsEnabled = s.systemNotificationsEnabled,
        onTogglePredicted = vm::togglePredicted,
        onToggleStart = vm::toggleStart,
        onToggleLate = vm::toggleLate,
        onOpenSystemSettings = {
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS,
            ).apply {
                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            }
            ctx.startActivity(intent)
        },
    )
}

@Composable
private fun BackupRoute() {
    val ctx = LocalContext.current
    val ep = remember {
        EntryPointAccessors.fromApplication(ctx.applicationContext, MainGraphEntryPoint::class.java)
    }
    val vm: BackupViewModel = viewModel(
        factory = simpleFactory {
            BackupViewModel(
                ctx = ctx.applicationContext,
                authMetaStore = ep.authMetaStore(),
                dbFile = ep.cyclesDbFile(),
            )
        },
    )
    val status by vm.status.collectAsStateWithLifecycle()
    BackupScreen(
        status = status,
        onExport = vm::export,
        onShare = vm::share,
        onImport = vm::importNotYetWired,
        onDismissStatus = vm::clearStatus,
    )
}

@Composable
private fun DuressRoute(nav: NavHostController) {
    val vm: DuressSetupViewModel = hiltViewModel()
    val result by vm.saveResult.collectAsStateWithLifecycle()
    DuressSetupScreen(onSave = { pin, mode -> vm.save(pin, mode) })
    LaunchedEffect(result) {
        if (result is DuressSetupViewModel.SaveResult.Success) {
            nav.popBackStack()
            vm.acknowledge()
        }
    }
}

@Composable
private fun ThreatPresetRoute(db: TidesDatabase, nav: NavHostController) {
    var initial by remember { mutableStateOf<ThreatPreset?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(db) {
        val name = withContext(Dispatchers.IO) { db.settingsDao().get("threat_preset") }
        initial = runCatching { ThreatPreset.valueOf(name ?: "") }
            .getOrDefault(ThreatPreset.DEFAULT)
    }
    val current = initial ?: return
    ThreatPresetScreen(
        initial = current,
        onContinue = { preset ->
            scope.launch {
                withContext(Dispatchers.IO) {
                    db.settingsDao().upsert(SettingsEntity("threat_preset", preset.name))
                }
                nav.popBackStack()
            }
        },
    )
}

private fun ThreatPreset.label(): String = when (this) {
    ThreatPreset.JUST_FOR_ME -> "Just for me"
    ThreatPreset.LOCKED_WHEN_AWAY -> "Locked when away"
    ThreatPreset.ALWAYS_LOCKED -> "Always locked"
}

private fun <T : ViewModel> simpleFactory(create: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <V : ViewModel> create(modelClass: Class<V>): V = create() as V
    }
