package ch.parkassist.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ch.parkassist.app.ui.screens.HomeScreen
import ch.parkassist.app.ui.screens.LogScreen

@Composable
fun ParkingNavHost(vm: ParkingViewModel = viewModel()) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(vm = vm, onNavigateToLog = { navController.navigate("log") })
        }
        composable("log") {
            LogScreen(vm = vm, onBack = { navController.popBackStack() })
        }
    }
}
