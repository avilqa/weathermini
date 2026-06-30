package com.example.weathermini.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.weathermini.ui.HistoryViewModel
import com.example.weathermini.ui.NotesViewModel
import com.example.weathermini.ui.WeatherDetailState
import com.example.weathermini.ui.WeatherViewModel
import com.example.weathermini.ui.screens.*

private data class BottomTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomTabs = listOf(
    BottomTab("search",   "Поиск",     Icons.Default.Search),
    BottomTab("favorites","Избранное", Icons.Default.Favorite),
    BottomTab("history",  "История",   Icons.Default.History),
    BottomTab("settings", "Настройки", Icons.Default.Settings),
)

private val noBottomBarRoutes = setOf(
    "detail/{lat}/{lon}/{name}",
    "notes/{cityName}/{lat}/{lon}/{temp}/{code}"
)

@Composable
fun WeatherNavGraph() {
    val navController = rememberNavController()
    val viewModel: WeatherViewModel = hiltViewModel()

    val currentEntry by navController.currentBackStackEntryAsState()
    val showBottomBar = currentEntry?.destination?.route !in noBottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentEntry?.destination?.route == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "search",
            modifier = Modifier.padding(innerPadding)
        ) {

            composable("search") {
                val state    by viewModel.searchUiState.collectAsState()
                val query    by viewModel.searchQuery.collectAsState()
                val sortMode by viewModel.sortMode.collectAsState()

                SearchScreen(
                    state = state,
                    query = query,
                    sortMode = sortMode,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onSortModeChange = viewModel::onSortModeChange,
                    onToggleFavourite = viewModel::toggleFavorite,
                    onCityClick = { city ->
                        navController.navigate(
                            "detail/${city.latitude}/${city.longitude}/${Uri.encode(city.name)}"
                        )
                    }
                )
            }

            composable("favorites") {
                val favorites by viewModel.favoriteCities.collectAsState()

                FavoritesScreen(
                    favorites = favorites,
                    onFavoriteClick = viewModel::toggleFavorite,
                    onBack = { navController.popBackStack() },
                    onCityClick = { city ->
                        navController.navigate(
                            "detail/${city.latitude}/${city.longitude}/${Uri.encode(city.name)}"
                        )
                    }
                )
            }

            composable(
                route = "detail/{lat}/{lon}/{name}",
                arguments = listOf(
                    navArgument("lat")  { type = NavType.FloatType },
                    navArgument("lon")  { type = NavType.FloatType },
                    navArgument("name") { type = NavType.StringType }
                )
            ) { backStack ->
                val lat  = backStack.arguments?.getFloat("lat")?.toDouble() ?: 0.0
                val lon  = backStack.arguments?.getFloat("lon")?.toDouble() ?: 0.0
                val name = backStack.arguments?.getString("name") ?: ""
                val settings by viewModel.settings.collectAsState()

                LaunchedEffect(lat, lon) { viewModel.loadWeather(lat, lon, name) }

                DetailScreen(
                    state    = viewModel.detailState,
                    settings = settings,
                    onBack   = { navController.popBackStack() },
                    onRetry  = { viewModel.loadWeather(lat, lon, name) },
                    onOpenNotes = { cityName ->
                        val weather = when (val s = viewModel.detailState) {
                            is WeatherDetailState.Success    -> s.weather
                            is WeatherDetailState.StaleError -> s.weather
                            else                              -> null
                        }
                        val temp = weather?.currentWeather?.temperature ?: 0.0
                        val code = weather?.currentWeather?.weatherCode ?: 0
                        navController.navigate(
                            "notes/${Uri.encode(cityName)}/$lat/$lon/$temp/$code"
                        )
                    }
                )
            }

            composable("history") {
                val historyViewModel: HistoryViewModel = hiltViewModel()
                val state by historyViewModel.uiState.collectAsState()
                val query by historyViewModel.query.collectAsState()

                HistoryScreen(
                    state = state,
                    query = query,
                    onQueryChange = historyViewModel::onQueryChange,
                    onClearHistory = historyViewModel::clearHistory,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "notes/{cityName}/{lat}/{lon}/{temp}/{code}",
                arguments = listOf(
                    navArgument("cityName") { type = NavType.StringType },
                    navArgument("lat")  { type = NavType.FloatType },
                    navArgument("lon")  { type = NavType.FloatType },
                    navArgument("temp") { type = NavType.FloatType },
                    navArgument("code") { type = NavType.IntType }
                )
            ) { backStack ->
                val temp = backStack.arguments?.getFloat("temp")?.toDouble() ?: 0.0
                val code = backStack.arguments?.getInt("code") ?: 0

                val notesViewModel: NotesViewModel = hiltViewModel()
                val state by notesViewModel.uiState.collectAsState()

                NotesScreen(
                    state = state,
                    onAddNote = { content -> notesViewModel.addNote(content, temp, code) },
                    onUpdateNote = { entity, text -> notesViewModel.updateNote(entity, text) },
                    onDeleteNote = notesViewModel::deleteNote,
                    onBack = { navController.popBackStack() }
                )
            }

            composable("settings") {
                val settings by viewModel.settings.collectAsState()

                SettingsScreen(
                    settings = settings,
                    onSetTemperatureUnit = viewModel::setTemperatureUnit,
                    onSetTheme = viewModel::setTheme,
                    onCacheTtlChanged = viewModel::setCacheTtlHours,
                    onBackgroundSyncChanged = viewModel::setBackgroundSyncEnabled,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}