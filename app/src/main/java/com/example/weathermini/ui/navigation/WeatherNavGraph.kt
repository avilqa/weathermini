package com.example.weathermini.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.weathermini.data.model.CityDto
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

private val noBottomBarRoutes = setOf("detail/{lat}/{lon}/{name}", "notes/{cityName}/{lat}/{lon}")

@Composable
fun WeatherNavGraph() {
    val navController = rememberNavController()
    val viewModel: WeatherViewModel = hiltViewModel()

    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val showBottomBar = currentRoute !in noBottomBarRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
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
                SearchScreen(
                    viewModel = viewModel,
                    onCityClick = { city ->
                        navController.navigate(
                            "detail/${city.latitude}/${city.longitude}/${city.name}"
                        )
                    },
                    onNavigateToFavorites = {
                        navController.navigate("favorites")
                    }
                )
            }

            composable("favorites") {
                val favorites by viewModel.favoriteCities.collectAsState()
                FavoritesScreen(
                    favorites = favorites,
                    onCityClick = { city ->
                        navController.navigate(
                            "detail/${city.latitude}/${city.longitude}/${city.name}"
                        )
                    },
                    onFavoriteClick = { city -> viewModel.toggleFavorite(city) },
                    onBack = { navController.popBackStack() }
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
                DetailScreen(
                    viewModel = viewModel,
                    lat = lat,
                    lon = lon,
                    cityName = name,
                    onBack = { navController.popBackStack() },
                    onOpenNotes = { cityName, noteLat, noteLon ->   // НОВОЕ
                        navController.navigate("notes/$cityName/$noteLat/$noteLon")
                    }
                )
            }

            composable("history") {
                HistoryScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "notes/{cityName}/{lat}/{lon}",
                arguments = listOf(
                    navArgument("cityName") { type = NavType.StringType },
                    navArgument("lat")      { type = NavType.FloatType },
                    navArgument("lon")      { type = NavType.FloatType }
                )
            ) { backStack ->
                val detailSuccess = viewModel.detailState as?
                        com.example.weathermini.ui.WeatherDetailState.Success
                NotesScreen(
                    onBack = { navController.popBackStack() },
                    temperature = detailSuccess?.weather?.currentWeather?.temperature ?: 0.0,
                    weatherCode = detailSuccess?.weather?.currentWeather?.weatherCode ?: 0
                )
            }

            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    viewModel = viewModel
                )
            }
        }
    }
}