package app.reelvocab.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import app.reelvocab.feature.deck.DeckRoute
import app.reelvocab.feature.deck.DeckScreen
import app.reelvocab.feature.deck.DeckUiState
import app.reelvocab.feature.library.LibraryRoute
import app.reelvocab.feature.library.LibraryScreen
import app.reelvocab.feature.settings.SettingsRoute
import app.reelvocab.feature.settings.SettingsScreen
import app.reelvocab.feature.today.TodayRoute
import app.reelvocab.feature.today.TodayScreen
import app.reelvocab.feature.today.TodayUiState
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
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val selectedTab = ReelTab.entries.firstOrNull { tab ->
        tab.routeClass.qualifiedName?.let { name -> currentRoute?.contains(name) } == true
    } ?: ReelTab.Today

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ReelTheme.colors.base,
        bottomBar = {
            ReelBottomBar(
                selectedTab = selectedTab,
                onSelect = { tab -> navController.navigateTab(tab) },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TodayRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<TodayRoute> {
                TodayScreen(
                    state = TodayUiState(),
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                    onOpenDeck = { navController.navigateTab(ReelTab.Deck) },
                )
            }

            composable<DeckRoute> { entry ->
                val route = entry.toRoute<DeckRoute>()
                DeckScreen(
                    state = DeckUiState(filter = route.filter),
                    onFilterChange = { filter ->
                        navController.navigate(DeckRoute(filter)) {
                            popUpTo<DeckRoute> { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onOpenLibrary = { navController.navigateTab(ReelTab.Library) },
                )
            }

            composable<LibraryRoute> {
                LibraryScreen(
                    onOpenDeck = { navController.navigateTab(ReelTab.Deck) },
                )
            }

            composable<SettingsRoute> {
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
