package app.reelvocab.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.reelvocab.BuildConfig
import app.reelvocab.R
import app.reelvocab.core.designsystem.theme.ReelTheme
import app.reelvocab.feature.capture.CaptureSessionService
import app.reelvocab.feature.deck.DeckRoute
import app.reelvocab.feature.deck.DeckScreen
import app.reelvocab.feature.library.LibraryRoute
import app.reelvocab.feature.library.LibraryScreen
import app.reelvocab.feature.media.MediaRoute
import app.reelvocab.feature.media.MediaScreen
import app.reelvocab.feature.media.PlayerRoute
import app.reelvocab.feature.media.PlayerScreen
import app.reelvocab.feature.settings.CloudSettingsUi
import app.reelvocab.feature.settings.SettingsRoute
import app.reelvocab.reminders.ReminderPrefs
import app.reelvocab.reminders.ReminderScheduler
import app.reelvocab.ui.CloudSettingsState
import app.reelvocab.ui.CloudSettingsViewModel
import app.reelvocab.feature.settings.SettingsScreen
import app.reelvocab.feature.today.ReviewRoute
import app.reelvocab.feature.today.ReviewScreen
import app.reelvocab.feature.today.TodayRoute
import app.reelvocab.feature.today.TodayScreen
import app.reelvocab.feature.today.TodayViewModel
import app.reelvocab.ui.DesignSystemGallery
import app.reelvocab.ui.DictionaryProbeRoute
import app.reelvocab.ui.DictionaryProbeScreen
import app.reelvocab.ui.DesignSystemRoute
import app.reelvocab.ui.LicensesRoute
import app.reelvocab.ui.LicensesScreen
import kotlin.reflect.KClass

private enum class ReelTab(
    val labelRes: Int,
    val icon: ImageVector,
    val route: Any,
    val routeClass: KClass<*>,
) {
    Today(R.string.tab_today, Icons.Filled.Home, TodayRoute, TodayRoute::class),
    Deck(R.string.tab_deck, Icons.Filled.Collections, DeckRoute(), DeckRoute::class),
    Library(R.string.tab_library, Icons.Filled.Book, LibraryRoute, LibraryRoute::class),
}

@Composable
fun MainScaffold(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectedTab = ReelTab.entries.firstOrNull { tab ->
        tab.routeClass.qualifiedName?.let { name -> currentRoute?.contains(name) } == true
    } ?: ReelTab.Today

    val showBottomBar = currentRoute?.contains(PlayerRoute::class.qualifiedName ?: "") != true

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ReelTheme.colors.base,
        bottomBar = {
            if (showBottomBar) {
                ReelBottomBar(
                    selectedTab = selectedTab,
                    onSelect = { tab -> navController.navigateTab(tab) },
                )
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
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                    onOpenDeck = { navController.navigateTab(ReelTab.Deck) },
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
                    onOpenLibrary = { navController.navigateTab(ReelTab.Library) },
                )
            }

            composable<LibraryRoute> {
                LibraryScreen(
                    onOpenDeck = { navController.navigateTab(ReelTab.Deck) },
                    onOpenMedia = { navController.navigate(MediaRoute) },
                )
            }

            composable<MediaRoute> {
                MediaScreen(
                    onBack = { navController.popBackStack() },
                    onPlay = { playerRoute -> navController.navigate(playerRoute) },
                )
            }

            composable<PlayerRoute> { entry ->
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
                    onEnableFloatingCapture = {
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
                    },
                    reminderEnabled = reminderEnabled,
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
                    cloud = CloudSettingsUi(
                        enabled = cloudState.enabled,
                        endpoint = cloudState.endpoint,
                        model = cloudState.model,
                        apiKey = cloudState.apiKey,
                    ),
                    onCloudChange = { ui ->
                        cloudViewModel.save(
                            CloudSettingsState(
                                enabled = ui.enabled,
                                endpoint = ui.endpoint,
                                model = ui.model,
                                apiKey = ui.apiKey,
                            ),
                        )
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
}

@Composable
private fun ReelBottomBar(
    selectedTab: ReelTab,
    onSelect: (ReelTab) -> Unit,
) {
    val colors = ReelTheme.colors
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
                    Icon(imageVector = tab.icon, contentDescription = null)
                },
                label = {
                    Text(
                        text = stringResource(tab.labelRes),
                        style = ReelTheme.type.label,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.gold,
                    selectedTextColor = colors.gold,
                    indicatorColor = colors.gold.copy(alpha = 0.14f),
                    unselectedIconColor = colors.textTertiary,
                    unselectedTextColor = colors.textTertiary,
                ),
            )
        }
    }
}

private fun NavController.navigateTab(tab: ReelTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
