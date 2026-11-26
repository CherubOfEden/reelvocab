package app.reelvocab.navigation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.reelvocab.BuildConfig
import app.reelvocab.R
import app.reelvocab.core.designsystem.component.ReelRule
import app.reelvocab.core.designsystem.icon.ReelIcons
import app.reelvocab.core.designsystem.theme.ReelTheme
import app.reelvocab.feature.capture.CapturePreferences
import app.reelvocab.feature.capture.CaptureSessionService
import app.reelvocab.feature.capture.PipCaptureMode
import app.reelvocab.feature.deck.DeckRoute
import app.reelvocab.feature.deck.DeckScreen
import app.reelvocab.feature.dictionary.DictionaryRoute
import app.reelvocab.feature.dictionary.DictionaryScreen
import app.reelvocab.feature.library.LibraryRoute
import app.reelvocab.feature.library.LibraryScreen
import app.reelvocab.feature.media.EditorRoute
import app.reelvocab.feature.media.MediaEditorScreen
import app.reelvocab.feature.media.MediaRoute
import app.reelvocab.feature.media.MediaScreen
import app.reelvocab.feature.media.PlayerRoute
import app.reelvocab.feature.media.PlayerScreen
import app.reelvocab.feature.media.PlayerTrack
import app.reelvocab.feature.notes.NotesRoute
import app.reelvocab.feature.notes.NotesScreen
import app.reelvocab.feature.settings.CloudSettingsUi
import app.reelvocab.feature.settings.PipMode
import app.reelvocab.feature.settings.SettingsRoute
import app.reelvocab.feature.settings.SettingsScreen
import app.reelvocab.feature.settings.ThemeMode
import app.reelvocab.feature.today.ReviewRoute
import app.reelvocab.feature.today.ReviewScreen
import app.reelvocab.feature.today.TodayRoute
import app.reelvocab.feature.today.TodayScreen
import app.reelvocab.feature.today.TodayViewModel
import app.reelvocab.reminders.FullScreenAlarms
import app.reelvocab.reminders.ReminderPrefs
import app.reelvocab.reminders.ReminderTimeFormat
import app.reelvocab.reminders.ReminderScheduler
import app.reelvocab.ui.CloudSettingsState
import app.reelvocab.ui.CloudSettingsViewModel
import app.reelvocab.ui.DesignSystemGallery
import app.reelvocab.ui.DesignSystemRoute
import app.reelvocab.ui.DictionaryProbeRoute
import app.reelvocab.ui.DictionaryProbeScreen
import app.reelvocab.ui.LicensesRoute
import app.reelvocab.ui.LicensesScreen
import kotlin.reflect.KClass
import kotlin.reflect.typeOf
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private enum class ReelTab(
    val labelRes: Int,
    val icon: ImageVector,
    val route: Any,
    val routeClass: KClass<*>,
) {
    Home(R.string.tab_home, ReelIcons.House, TodayRoute, TodayRoute::class),
    Deck(R.string.tab_deck, ReelIcons.Layers, DeckRoute(), DeckRoute::class),
    Dictionary(R.string.tab_dictionary, ReelIcons.BookA, DictionaryRoute, DictionaryRoute::class),
    Library(R.string.tab_library, ReelIcons.Library, LibraryRoute, LibraryRoute::class),
    Notes(R.string.tab_notes, ReelIcons.NotebookPen, NotesRoute, NotesRoute::class),
}

private object PlayerTrackListNavType : NavType<List<PlayerTrack>>(isNullableAllowed = false) {
    override val name: String = "List<PlayerTrack>"

    override fun put(bundle: Bundle, key: String, value: List<PlayerTrack>) {
        bundle.putString(key, Json.encodeToString<List<PlayerTrack>>(value))
    }

    override fun get(bundle: Bundle, key: String): List<PlayerTrack>? =
        bundle.getString(key)?.let { Json.decodeFromString<List<PlayerTrack>>(it) }

    override fun parseValue(value: String): List<PlayerTrack> =
        Json.decodeFromString<List<PlayerTrack>>(value)

    override fun serializeAsValue(value: List<PlayerTrack>): String =
        Uri.encode(Json.encodeToString<List<PlayerTrack>>(value))
}

@Composable
fun MainScaffold(
    modifier: Modifier = Modifier,
    themeMode: ThemeMode = ThemeMode.System,
    onThemeModeChange: ((ThemeMode) -> Unit)? = null,
    openReviewRequest: Boolean = false,
    onReviewRequestHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var showAlarmPicker by remember { mutableStateOf(false) }
    var pipMode by remember { mutableStateOf(CapturePreferences.pipMode(context)) }
    var cropOverlayEnabled by remember {
        mutableStateOf(CapturePreferences.showCropOverlay(context))
    }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    LaunchedEffect(openReviewRequest) {
        if (openReviewRequest) {
            navController.navigate(ReviewRoute) {
                launchSingleTop = true
            }
            onReviewRequestHandled()
        }
    }
    val selectedTab = ReelTab.entries.firstOrNull { tab ->
        tab.routeClass.qualifiedName?.let { name -> currentRoute?.contains(name) } == true
    } ?: ReelTab.Home

    val showBottomBar =
        currentRoute?.contains(PlayerRoute::class.qualifiedName ?: "") != true &&
            currentRoute?.contains(EditorRoute::class.qualifiedName ?: "") != true
    val pipActive by CaptureSessionService.runningFlow.collectAsStateWithLifecycle()
    val startPip: () -> Unit = {
        if (Settings.canDrawOverlays(context)) {
            CaptureSessionService.startPill(context)
        } else {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}"),
                ),
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ReelTheme.colors.base,
        bottomBar = {
            if (showBottomBar) {
                ReelBottomBar(
                    selectedTab = selectedTab,
                    onSelect = { tab -> navController.navigateTab(tab, currentRoute) },
                )
            }
        },
        floatingActionButton = {
            if (showBottomBar && selectedTab == ReelTab.Home) {
                FloatingActionButton(
                    onClick = {
                        if (pipActive) {
                            CaptureSessionService.requestCapture(context)
                        } else {
                            startPip()
                        }
                    },
                    containerColor = ReelTheme.colors.accent,
                    contentColor = ReelTheme.colors.onAccent,
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = ReelIcons.ScanText,
                        contentDescription = stringResource(R.string.capture_fab),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TodayRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<TodayRoute> {
                val todayViewModel: TodayViewModel = hiltViewModel()
                val todayState by todayViewModel.state.collectAsStateWithLifecycle()
                LaunchedEffect(Unit) {
                    todayViewModel.refresh()
                }
                TodayScreen(
                    state = todayState,
                    pipActive = pipActive,
                    onTogglePip = {
                        if (pipActive) {
                            CaptureSessionService.stop(context)
                        } else {
                            startPip()
                        }
                    },
                    onOpenAlarm = { showAlarmPicker = true },
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                    onOpenDeck = { navController.navigateTab(ReelTab.Deck, currentRoute) },
                    onStartReview = { navController.navigate(ReviewRoute) },
                )
            }

            composable<ReviewRoute> {
                ReviewScreen(onBack = { navController.popBackStack() })
            }

            composable<DeckRoute> { entry ->
                val route = entry.toRoute<DeckRoute>()
                DeckScreen(
                    initialFilter = route.filter,
                    onOpenLibrary = { navController.navigateTab(ReelTab.Library, currentRoute) },
                )
            }

            composable<DictionaryRoute> {
                DictionaryScreen()
            }

            composable<LibraryRoute> {
                LibraryScreen(
                    onOpenDeck = { navController.navigateTab(ReelTab.Deck, currentRoute) },
                    onPlay = { uri, title, kind ->
                        navController.navigate(
                            PlayerRoute(uri = uri, title = title, kind = kind),
                        )
                    },
                    onEdit = { uri, title, kind ->
                        navController.navigate(
                            EditorRoute(uri = uri, title = title, kind = kind),
                        )
                    },
                    onPlayMedia = { playerRoute -> navController.navigate(playerRoute) },
                )
            }

            composable<EditorRoute> { entry ->
                val route = entry.toRoute<EditorRoute>()
                MediaEditorScreen(
                    route = route,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }

            composable<NotesRoute> {
                NotesScreen()
            }

            composable<MediaRoute> {
                MediaScreen(
                    onPlay = { playerRoute -> navController.navigate(playerRoute) },
                )
            }

            composable<PlayerRoute>(
                typeMap = mapOf(typeOf<List<PlayerTrack>>() to PlayerTrackListNavType),
            ) { entry ->
                val route = entry.toRoute<PlayerRoute>()
                PlayerScreen(
                    route = route,
                    onBack = { navController.popBackStack() },
                )
            }

            composable<SettingsRoute> {
                val cloudViewModel: CloudSettingsViewModel = hiltViewModel()
                val cloudState by cloudViewModel.state.collectAsStateWithLifecycle()
                var reminderEnabled by remember {
                    mutableStateOf(ReminderPrefs.isEnabled(context))
                }
                var reminderHour by remember {
                    mutableIntStateOf(ReminderPrefs.hour(context))
                }
                var reminderMinute by remember {
                    mutableIntStateOf(ReminderPrefs.minute(context))
                }
                var reminderIs24Hour by remember {
                    mutableStateOf(ReminderPrefs.is24Hour(context))
                }
                var fullScreenAlarm by remember {
                    mutableStateOf(ReminderPrefs.fullScreen(context))
                }
                var fullScreenAvailable by remember {
                    mutableStateOf(FullScreenAlarms.isAllowed(context))
                }
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            fullScreenAvailable = FullScreenAlarms.isAllowed(context)
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }
                val notificationPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { }
                SettingsScreen(
                    onOpenLicenses = { navController.navigate(LicensesRoute) },
                    onOpenDesignSystem = if (BuildConfig.DEBUG) {
                        { navController.navigate(DesignSystemRoute) }
                    } else {
                        null
                    },
                    onOpenDictionaryProbe = if (BuildConfig.DEBUG) {
                        { navController.navigate(DictionaryProbeRoute) }
                    } else {
                        null
                    },
                    onEnableFloatingCapture = startPip,
                    pipMode = pipMode.toSettingsMode(),
                    onPipModeChange = { mode ->
                        val captureMode = mode.toCaptureMode()
                        CapturePreferences.setPipMode(context, captureMode)
                        pipMode = captureMode
                    },
                    cropOverlayEnabled = cropOverlayEnabled,
                    onToggleCropOverlay = {
                        val enabled = !cropOverlayEnabled
                        CapturePreferences.setShowCropOverlay(context, enabled)
                        cropOverlayEnabled = enabled
                    },
                    overlayAvailable = Settings.canDrawOverlays(context),
                    onOpenOverlaySettings = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}"),
                                ),
                            )
                        }
                    },
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange,
                    reminderEnabled = reminderEnabled,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute,
                    reminderIs24Hour = reminderIs24Hour,
                    onToggleReminder = {
                        val enabled = !reminderEnabled
                        ReminderPrefs.setEnabled(context, enabled)
                        if (enabled) {
                            if (
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS,
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermission.launch(
                                    Manifest.permission.POST_NOTIFICATIONS,
                                )
                            }
                            ReminderScheduler.schedule(context)
                        } else {
                            ReminderScheduler.cancel(context)
                        }
                        reminderEnabled = enabled
                    },
                    onReminderTimeChange = { hour, minute, is24Hour ->
                        ReminderPrefs.setTime(context, hour, minute)
                        ReminderPrefs.setTimeFormat(
                            context,
                            if (is24Hour) {
                                ReminderTimeFormat.TwentyFourHour
                            } else {
                                ReminderTimeFormat.TwelveHour
                            },
                        )
                        reminderHour = hour
                        reminderMinute = minute
                        reminderIs24Hour = is24Hour
                        if (reminderEnabled) {
                            ReminderScheduler.schedule(context)
                        }
                    },
                    fullScreenAlarm = fullScreenAlarm,
                    fullScreenAlarmAvailable = fullScreenAvailable,
                    onToggleFullScreenAlarm = {
                        val enabled = !fullScreenAlarm
                        ReminderPrefs.setFullScreen(context, enabled)
                        fullScreenAlarm = enabled
                        fullScreenAvailable = FullScreenAlarms.isAllowed(context)
                    },
                    onOpenFullScreenAlarmSettings = {
                        FullScreenAlarms.settingsIntent(context)?.let { intent ->
                            runCatching { context.startActivity(intent) }
                        }
                    },
                    appVersion = BuildConfig.VERSION_NAME,
                    cloud = CloudSettingsUi(enabled = cloudState.enabled),
                    onCloudChange = { ui ->
                        cloudViewModel.save(CloudSettingsState(enabled = ui.enabled))
                    },
                )
            }

            composable<LicensesRoute> {
                LicensesScreen(onBack = { navController.popBackStack() })
            }

            if (BuildConfig.DEBUG) {
                composable<DesignSystemRoute> {
                    DesignSystemGallery(onBack = { navController.popBackStack() })
                }
                composable<DictionaryProbeRoute> {
                    DictionaryProbeScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }

    if (showAlarmPicker) {
        QuickAlarmDialog(
            initialHour = ReminderPrefs.hour(context),
            initialMinute = ReminderPrefs.minute(context),
            initialIs24Hour = ReminderPrefs.is24Hour(context),
            onConfirm = { hour, minute, is24Hour ->
                ReminderPrefs.setTime(context, hour, minute)
                ReminderPrefs.setTimeFormat(
                    context,
                    if (is24Hour) {
                        ReminderTimeFormat.TwentyFourHour
                    } else {
                        ReminderTimeFormat.TwelveHour
                    },
                )
                ReminderPrefs.setEnabled(context, true)
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                ReminderScheduler.schedule(context)
                showAlarmPicker = false
            },
            onDismiss = { showAlarmPicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAlarmDialog(
    initialHour: Int,
    initialMinute: Int,
    initialIs24Hour: Boolean,
    onConfirm: (Int, Int, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = ReelTheme.colors
    var is24Hour by remember { mutableStateOf(initialIs24Hour) }
    var hour by remember { mutableIntStateOf(initialHour) }
    var minute by remember { mutableIntStateOf(initialMinute) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.elevated,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary,
        title = {
            Text(
                text = stringResource(R.string.quick_alarm_title),
                style = ReelTheme.type.titleM,
            )
        },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(ReelTheme.spacing.s)) {
                    TextButton(onClick = { is24Hour = false }) {
                        Text(
                            text = stringResource(R.string.quick_alarm_format_12),
                            style = ReelTheme.type.label,
                            color = if (!is24Hour) colors.accent else colors.textSecondary,
                        )
                    }
                    TextButton(onClick = { is24Hour = true }) {
                        Text(
                            text = stringResource(R.string.quick_alarm_format_24),
                            style = ReelTheme.type.label,
                            color = if (is24Hour) colors.accent else colors.textSecondary,
                        )
                    }
                }
                key(is24Hour) {
                    val picker = rememberTimePickerState(
                        initialHour = hour,
                        initialMinute = minute,
                        is24Hour = is24Hour,
                    )
                    LaunchedEffect(picker.hour, picker.minute) {
                        hour = picker.hour
                        minute = picker.minute
                    }
                    TimePicker(state = picker)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(hour, minute, is24Hour) }) {
                Text(
                    text = stringResource(R.string.quick_alarm_save),
                    style = ReelTheme.type.label,
                    color = colors.accent,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.quick_alarm_cancel),
                    style = ReelTheme.type.label,
                    color = colors.textSecondary,
                )
            }
        },
    )
}

@Composable
private fun ReelBottomBar(
    selectedTab: ReelTab,
    onSelect: (ReelTab) -> Unit,
) {
    val colors = ReelTheme.colors
    Column(modifier = Modifier.background(colors.base)) {
        ReelRule()
        NavigationBar(
            containerColor = colors.surface,
            contentColor = colors.textPrimary,
            tonalElevation = 0.dp,
        ) {
            ReelTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = selectedTab == tab,
                    onClick = { onSelect(tab) },
                    icon = {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(tab.labelRes),
                            style = ReelTheme.type.navLabel,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.accent,
                        selectedTextColor = colors.accent,
                        indicatorColor = colors.accent.copy(alpha = 0.10f),
                        unselectedIconColor = colors.textTertiary,
                        unselectedTextColor = colors.textTertiary,
                    ),
                )
            }
        }
    }
}

private fun NavController.navigateTab(tab: ReelTab, currentRoute: String?) {
    val onTopLevelDestination = ReelTab.entries.any { candidate ->
        candidate.routeClass.qualifiedName?.let { name -> currentRoute?.contains(name) } == true
    }
    if (!onTopLevelDestination) {
        popBackStack(graph.findStartDestination().id, inclusive = false)
    }
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) {
            inclusive = false
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun PipCaptureMode.toSettingsMode(): PipMode = when (this) {
    PipCaptureMode.WordsOnly -> PipMode.WordsOnly
    PipCaptureMode.Video -> PipMode.Video
    PipCaptureMode.Audio -> PipMode.Audio
    PipCaptureMode.All -> PipMode.All
}

private fun PipMode.toCaptureMode(): PipCaptureMode = when (this) {
    PipMode.WordsOnly -> PipCaptureMode.WordsOnly
    PipMode.Video -> PipCaptureMode.Video
    PipMode.Audio -> PipCaptureMode.Audio
    PipMode.All -> PipCaptureMode.All
}
