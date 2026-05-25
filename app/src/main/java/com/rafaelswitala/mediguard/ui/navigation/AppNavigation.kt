package com.rafaelswitala.mediguard.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.rafaelswitala.mediguard.ui.screens.AddMedicationScreen
import com.rafaelswitala.mediguard.ui.screens.HistoryScreen
import com.rafaelswitala.mediguard.ui.screens.HomeScreen
import com.rafaelswitala.mediguard.ui.screens.MedicationsScreen
import com.rafaelswitala.mediguard.ui.screens.SettingsScreen
import com.rafaelswitala.mediguard.viewmodel.HistoryViewModel
import com.rafaelswitala.mediguard.viewmodel.MedicationViewModel
import com.rafaelswitala.mediguard.viewmodel.SettingsViewModel

/**
 * Navigation routes for the app
 */
sealed class Route(val route: String) {
    data object Home : Route("home")
    data object Medications : Route("medications")
    data object AddMedication : Route("add_medication")
    data object EditMedication : Route("edit_medication/{medicationId}") {
        fun createRoute(medicationId: Long): String = "edit_medication/$medicationId"
    }
    data object History : Route("history")
    data object Settings : Route("settings")
}

/**
 * App navigation host with all screens
 */
@Composable
fun AppNavigation(navController: NavHostController) {
    val medicationViewModel: MedicationViewModel = hiltViewModel()
    val historyViewModel: HistoryViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = Route.Home.route
    ) {
        composable(Route.Home.route) {
            HomeScreen(
                viewModel = medicationViewModel,
                historyViewModel = historyViewModel,
                onNavigateToMedications = {
                    navController.navigate(Route.Medications.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToHistory = {
                    navController.navigate(Route.History.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Route.Settings.route)
                }
            )
        }

        composable(Route.Medications.route) {
            MedicationsScreen(
                viewModel = medicationViewModel,
                onNavigateToAdd = {
                    navController.navigate(Route.AddMedication.route)
                },
                onNavigateHome = {
                    navController.navigate(Route.Home.route) {
                        popUpTo(Route.Home.route)
                        launchSingleTop = true
                    }
                },
                onNavigateHistory = {
                    navController.navigate(Route.History.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateSettings = {
                    navController.navigate(Route.Settings.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateToEdit = { medicationId ->
                    navController.navigate(Route.EditMedication.createRoute(medicationId))
                }
            )
        }

        composable(Route.AddMedication.route) {
            AddMedicationScreen(
                viewModel = medicationViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Route.EditMedication.route,
            arguments = listOf(navArgument("medicationId") { type = NavType.LongType })
        ) { backStackEntry ->
            AddMedicationScreen(
                viewModel = medicationViewModel,
                medicationId = backStackEntry.arguments?.getLong("medicationId"),
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Route.History.route) {
            HistoryScreen(
                viewModel = historyViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateHome = {
                    navController.navigate(Route.Home.route) {
                        popUpTo(Route.Home.route)
                        launchSingleTop = true
                    }
                },
                onNavigateMedications = {
                    navController.navigate(Route.Medications.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateSettings = {
                    navController.navigate(Route.Settings.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Route.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateHome = {
                    navController.navigate(Route.Home.route) {
                        popUpTo(Route.Home.route)
                        launchSingleTop = true
                    }
                },
                onNavigateMedications = {
                    navController.navigate(Route.Medications.route) {
                        launchSingleTop = true
                    }
                },
                onNavigateHistory = {
                    navController.navigate(Route.History.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
