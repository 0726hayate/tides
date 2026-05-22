@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.hayate0726.tides.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.hayate0726.tides.ui.settings.AppearanceRepository
import com.hayate0726.tides.ui.settings.AppearanceScreen
import com.hayate0726.tides.ui.settings.BackupScreen
import com.hayate0726.tides.ui.settings.BackupViewModel
import com.hayate0726.tides.ui.settings.BiometricToggleScreen
import com.hayate0726.tides.ui.settings.BiometricToggleViewModel
import com.hayate0726.tides.ui.settings.BirthControlScreen
import com.hayate0726.tides.ui.settings.BirthControlViewModel
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
fun MainScaffold(appViewModel: AppViewModel, db: TidesDatabase, rootNav: NavHostController) {
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
            composable(Routes.Settings) { SettingsRoute(appViewModel, db, nav, rootNav) }
            composable(Routes.SettingsNotifications) { NotificationsRoute(db, nav) }
            composable(Routes.SettingsBackup) { BackupRoute(appViewModel, nav) }
            composable(Routes.SettingsDuress) { DuressRoute(nav) }
            composable(Routes.SettingsThreatPreset) { ThreatPresetRoute(db, nav) }
            composable(Routes.SettingsFeedback) {
                SubScreenScaffold("Send feedback", nav) { p ->
                    Box(modifier = Modifier.padding(p)) {
                        FeedbackScreen()
                    }
                }
            }
            composable(Routes.SettingsBiometric) { BiometricRoute(nav) }
            composable(Routes.SettingsBirthControl) { BirthControlRoute(db, nav) }
            composable(Routes.SettingsAppearance) { AppearanceRoute(nav) }
            composable(Routes.SettingsGoals) { GoalsEditorRoute(db, nav) }
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
    val ctx = LocalContext.current
    val widgetUpdater = remember {
        EntryPointAccessors.fromApplication(ctx.applicationContext, MainGraphEntryPoint::class.java)
            .widgetUpdater()
    }
    val userPrivacyRepository = remember {
        EntryPointAccessors.fromApplication(ctx.applicationContext, MainGraphEntryPoint::class.java)
            .userPrivacyRepository()
    }
    val vm: CalendarViewModel = viewModel(
        key = "calendar-${System.identityHashCode(db)}",
        factory = simpleFactory {
            CalendarViewModel(db, widgetUpdater, userPrivacyRepository)
        },
    )
    val logVm: LogViewModel = viewModel(
        key = "log-${System.identityHashCode(db)}",
        factory = simpleFactory { LogViewModel(db) },
    )
    val ui by vm.state.collectAsStateWithLifecycle()
    val logState by logVm.state.collectAsStateWithLifecycle()
    var pendingLogDate by remember { mutableStateOf<java.time.LocalDate?>(null) }

    // Re-read goals/BC/cycles whenever the Calendar tab regains focus, so
    // changes made elsewhere (e.g., toggling AVOID_PREGNANCY in Settings →
    // Goals) reflect immediately instead of requiring an app restart.
    LaunchedEffect(Unit) { vm.refresh() }

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
            predictedPeriodRanges = ui.predictedPeriodRanges,
            ovulationRanges = ui.ovulationRanges,
            follicularRanges = ui.follicularRanges,
            lutealRanges = ui.lutealRanges,
            symptomDays = ui.symptomDays,
        ),
        view = ui.view,
        onViewChange = vm::changeView,
        onDayClick = {
            pendingLogDate = it
            logVm.load(it)
        },
        onPreviousMonth = vm::previousMonth,
        onNextMonth = vm::nextMonth,
        onGoToToday = vm::goToToday,
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
    val ctx = LocalContext.current
    val vm: StatsViewModel = viewModel(
        key = "stats-${System.identityHashCode(db)}",
        factory = simpleFactory { StatsViewModel(db) },
    )
    val ui by vm.state.collectAsStateWithLifecycle()
    val range by vm.range.collectAsStateWithLifecycle()
    val state = ui
    if (state == null) {
        Text("Loading…", modifier = Modifier.padding(24.dp))
        return
    }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    StatsScreen(
        state = state,
        range = range,
        onRangeChange = vm::setRange,
        onDismissInsight = vm::dismissInsight,
        onExportPdf = {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                shareDoctorPdf(ctx.applicationContext, db, range)
            }
        },
        onExportCsv = {
            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                shareCsvExport(ctx.applicationContext, db, range)
            }
        },
    )
}

private suspend fun shareDoctorPdf(
    appCtx: android.content.Context,
    db: TidesDatabase,
    range: StatsViewModel.Range,
) {
    val to = java.time.LocalDate.now()
    val from = range.months?.let { to.minusMonths(it.toLong()) } ?: java.time.LocalDate.of(2000, 1, 1)
    val entries = db.cycleEntryDao().rangeOnce(from, to)
    val activeBc = db.birthControlDao().activeOnce()?.method
    val cycles = com.hayate0726.tides.domain.CycleDetector.detect(
        entries.map { com.hayate0726.tides.domain.CycleDetector.Entry(it.date, it.flowIntensity) },
        activeBirthControl = activeBc,
    )
    val cycleStats = com.hayate0726.tides.domain.CycleStats.compute(cycles)
    val flowEntries = entries.map {
        com.hayate0726.tides.domain.FigoAnalysis.FlowEntry(it.date, it.flowIntensity)
    }
    val figo = com.hayate0726.tides.domain.FigoAnalysis.analyze(
        cycles = cycles,
        cycleFlowEntries = flowEntries,
        painEntries = entries.mapNotNull { e ->
            e.painSeverity?.let { com.hayate0726.tides.domain.FigoAnalysis.PainEntry(e.date, it) }
        },
        intermenstrualBleedingDates = emptyList(),
        today = to,
    )
    val bytes = java.io.ByteArrayOutputStream().also {
        com.hayate0726.tides.ui.export.DoctorPdfBuilder.build(
            cycles = cycles,
            stats = cycleStats,
            figoPatterns = figo,
            userName = null,
            userDob = null,
            rangeStart = from,
            rangeEnd = to,
            appVersion = "0.1.0",
            output = it,
        )
    }.toByteArray()
    com.hayate0726.tides.ui.export.Sharer.sharePdf(
        appCtx, bytes,
        displayName = "tides-cycle-summary-${java.time.LocalDate.now()}.pdf",
    )
}

private suspend fun shareCsvExport(
    appCtx: android.content.Context,
    db: TidesDatabase,
    range: StatsViewModel.Range,
) {
    val to = java.time.LocalDate.now()
    val from = range.months?.let { to.minusMonths(it.toLong()) } ?: java.time.LocalDate.of(2000, 1, 1)
    val entries = db.cycleEntryDao().rangeOnce(from, to)
    val activeBc = db.birthControlDao().activeOnce()?.method
    val cycles = com.hayate0726.tides.domain.CycleDetector.detect(
        entries.map { com.hayate0726.tides.domain.CycleDetector.Entry(it.date, it.flowIntensity) },
        activeBirthControl = activeBc,
    )
    val symptomRows = db.symptomEntryDao().rangeOnce(from, to)
    val symptomsByDate: Map<java.time.LocalDate, List<com.hayate0726.tides.domain.model.Symptom>> =
        symptomRows.groupBy({ it.date }, { it.symptom })
    val notesByDate: Map<java.time.LocalDate, String> =
        entries.mapNotNull { e -> e.notes?.let { n -> e.date to n } }.toMap()
    val csv = com.hayate0726.tides.ui.export.CsvBuilder.build(cycles, symptomsByDate, notesByDate)
    com.hayate0726.tides.ui.export.Sharer.shareCsv(
        appCtx, csv,
        displayName = "tides-export-${java.time.LocalDate.now()}.csv",
    )
}

@Composable
private fun SettingsRoute(appViewModel: AppViewModel, db: TidesDatabase, nav: NavHostController, rootNav: NavHostController) {
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

    val hasSnapshot by appViewModel.hasPreImportSnapshotFlow.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { appViewModel.refreshPreImportSnapshotState() }
    var showRollbackDialog by remember { mutableStateOf(false) }
    val rollbackScope = androidx.compose.runtime.rememberCoroutineScope()

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
        onBiometric = { nav.navigate(Routes.SettingsBiometric) },
        onBirthControl = { nav.navigate(Routes.SettingsBirthControl) },
        onAppearance = { nav.navigate(Routes.SettingsAppearance) },
        onGoals = { nav.navigate(Routes.SettingsGoals) },
        onImport = { rootNav.navigate(Routes.Import) },
        onRollback = { showRollbackDialog = true },
        showRollback = hasSnapshot,
    )

    if (showRollbackDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showRollbackDialog = false },
            title = { Text("Roll back last import?") },
            text = {
                Text(
                    "This will replace your current data with the snapshot from before your last import. " +
                        "Anything you added since then will be lost.",
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showRollbackDialog = false
                    rollbackScope.launch {
                        appViewModel.rollbackLastImport()
                        appViewModel.refreshPreImportSnapshotState()
                    }
                }) { Text("Roll back") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showRollbackDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun NotificationsRoute(db: TidesDatabase, nav: NavHostController) {
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
    SubScreenScaffold("Reminders", nav) { padding ->
        Box(modifier = Modifier.padding(padding)) {
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
    }
}

@Composable
private fun BackupRoute(appViewModel: AppViewModel, nav: NavHostController) {
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
                appViewModel = appViewModel,
            )
        },
    )
    val status by vm.status.collectAsStateWithLifecycle()
    SubScreenScaffold("Backup & restore", nav) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            BackupScreen(
                status = status,
                onExport = vm::export,
                onShare = vm::share,
                onRestore = vm::restore,
                onDismissStatus = vm::clearStatus,
            )
        }
    }
}

@Composable
private fun DuressRoute(nav: NavHostController) {
    val vm: DuressSetupViewModel = hiltViewModel()
    val result by vm.saveResult.collectAsStateWithLifecycle()
    SubScreenScaffold("Duress PIN", nav) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            DuressSetupScreen(onSave = { pin, mode -> vm.save(pin, mode) })
        }
    }
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
    SubScreenScaffold("Privacy preset", nav) { padding ->
        Box(modifier = Modifier.padding(padding)) {
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
    }
}

@Composable
private fun BiometricRoute(nav: NavHostController) {
    val ctx = LocalContext.current
    val ep = remember {
        EntryPointAccessors.fromApplication(ctx.applicationContext, MainGraphEntryPoint::class.java)
    }
    val vm: BiometricToggleViewModel = viewModel(
        factory = simpleFactory {
            BiometricToggleViewModel(
                authMetaStore = ep.authMetaStore(),
                biometricKeyStore = ep.biometricKeyStore(),
            )
        },
    )
    val enrolled by vm.enrolled.collectAsStateWithLifecycle()
    val status by vm.status.collectAsStateWithLifecycle()

    val activity = ctx as? androidx.fragment.app.FragmentActivity
    LaunchedEffect(status) {
        if (status == BiometricToggleViewModel.Status.AwaitingBiometric) {
            if (activity == null) {
                vm.cancelPending("This screen needs a FragmentActivity to show the biometric prompt.")
                return@LaunchedEffect
            }
            com.hayate0726.tides.ui.lock.BiometricController.authenticate(
                activity = activity,
                title = "Enable biometric unlock",
                subtitle = "Confirm your biometric to wrap your encryption key.",
                negativeButton = "Cancel",
                onSuccess = { vm.completeEnroll() },
                onError = { msg -> vm.cancelPending(msg) },
            )
        }
    }

    SubScreenScaffold("Biometric unlock", nav) { padding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(padding)) {
            BiometricToggleScreen(
                enrolled = enrolled,
                status = status,
                onEnable = vm::prepare,
                onDisable = vm::disable,
                onDismissStatus = vm::clearStatus,
            )
        }
    }
}

@Composable
private fun BirthControlRoute(db: TidesDatabase, nav: NavHostController) {
    val ctx = LocalContext.current
    val ep = remember {
        EntryPointAccessors.fromApplication(ctx.applicationContext, MainGraphEntryPoint::class.java)
    }
    val vm: BirthControlViewModel = viewModel(
        key = "bc-${System.identityHashCode(db)}",
        factory = simpleFactory {
            BirthControlViewModel(db, ep.userPrivacyRepository(), ep.widgetUpdater())
        },
    )
    val s by vm.state.collectAsStateWithLifecycle()
    SubScreenScaffold("Birth control", nav) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            BirthControlScreen(state = s, onSelect = vm::select, onSave = vm::save)
        }
    }
    LaunchedEffect(s.saved) {
        if (s.saved) nav.popBackStack()
    }
}

@Composable
private fun GoalsEditorRoute(db: TidesDatabase, nav: NavHostController) {
    val ctx = LocalContext.current
    val ep = remember {
        EntryPointAccessors.fromApplication(ctx.applicationContext, MainGraphEntryPoint::class.java)
    }
    val vm: com.hayate0726.tides.ui.settings.GoalsEditorViewModel = viewModel(
        key = "goals-${System.identityHashCode(db)}",
        factory = simpleFactory {
            com.hayate0726.tides.ui.settings.GoalsEditorViewModel(db, ep.userPrivacyRepository())
        },
    )
    val s by vm.state.collectAsStateWithLifecycle()
    SubScreenScaffold("Goals", nav) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            com.hayate0726.tides.ui.settings.GoalsEditorScreen(
                selected = s.selected,
                onToggle = vm::toggle,
                onSave = vm::save,
            )
        }
    }
    LaunchedEffect(s.saved) {
        if (s.saved) nav.popBackStack()
    }
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

@Composable
private fun AppearanceRoute(nav: NavHostController) {
    val ctx = LocalContext.current
    val ep = remember {
        EntryPointAccessors.fromApplication(ctx.applicationContext, MainGraphEntryPoint::class.java)
    }
    val repo = ep.appearanceRepository()
    val useDynamic by repo.useDynamicColor.collectAsStateWithLifecycle()
    val themeMode by repo.themeMode.collectAsStateWithLifecycle()
    val shufflePin by repo.shufflePinKeypad.collectAsStateWithLifecycle()
    SubScreenScaffold("Appearance", nav) { p ->
        Box(modifier = Modifier.padding(p)) {
            AppearanceScreen(
                themeMode = themeMode,
                onThemeModeChange = repo::setThemeMode,
                useDynamicColor = useDynamic,
                onToggleDynamicColor = repo::setUseDynamicColor,
                shufflePinKeypad = shufflePin,
                onToggleShufflePinKeypad = repo::setShufflePinKeypad,
            )
        }
    }
}


@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SubScreenScaffold(
    title: String,
    nav: NavHostController,
    content: @Composable (PaddingValues) -> Unit,
) {
    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text(title) },
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
