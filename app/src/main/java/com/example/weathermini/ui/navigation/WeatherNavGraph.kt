package com.example.weathermini.ui.navigation

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.weathermini.ui.WeatherViewModel
import com.example.weathermini.ui.screens.DetailScreen
import com.example.weathermini.ui.screens.FavoritesScreen
import com.example.weathermini.ui.screens.SearchScreen

@Composable
fun WeatherNavGraph() {
    val navController = rememberNavController()
    val viewModel: WeatherViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = "search") {

        composable("search") {
            SearchScreen(
                viewModel = viewModel,
                onCityClick = { city ->
                    navController.navigate("detail/${city.latitude}/${city.longitude}/${city.name}")
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
                    navController.navigate("detail/${city.latitude}/${city.longitude}/${city.name}")
                },
                onFavoriteClick = { city -> viewModel.toggleFavorite(city) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "detail/{lat}/{lon}/{name}",
            arguments = listOf(
                navArgument("lat") { type = NavType.FloatType },
                navArgument("lon") { type = NavType.FloatType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getFloat("lat")?.toDouble() ?: 0.0
            val lon = backStackEntry.arguments?.getFloat("lon")?.toDouble() ?: 0.0
            val name = backStackEntry.arguments?.getString("name") ?: ""
            DetailScreen(
                viewModel = viewModel,
                lat = lat,
                lon = lon,
                cityName = name,
                onBack = { navController.popBackStack() }
            )
        }
    }
}