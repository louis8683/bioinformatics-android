package com.louislu.pennbioinformatics

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.louislu.pennbioinformatics.auth.AuthViewModel
import com.louislu.pennbioinformatics.ble.BleViewModel
import com.louislu.pennbioinformatics.screen.connect.ConnectDeviceScreenRoot
import com.louislu.pennbioinformatics.screen.monitor.DataMonitorScreenRoot
import com.louislu.pennbioinformatics.screen.monitor.DataMonitorViewModel
import com.louislu.pennbioinformatics.screen.history.EntryHistoryScreenRoot
import com.louislu.pennbioinformatics.screen.history.EntryHistoryViewModel
import com.louislu.pennbioinformatics.screen.login.LoginScreenRoot
import com.louislu.pennbioinformatics.screen.menu.MenuScreenRoot
import com.louislu.pennbioinformatics.screen.permission.PermissionScreenRoot
import com.louislu.pennbioinformatics.screen.group.SelectGroupScreenRoot
import com.louislu.pennbioinformatics.screen.history.SessionHistoryScreenRoot
import com.louislu.pennbioinformatics.screen.splash.SplashScreenRoot
import timber.log.Timber

@Composable
fun NavigationRoot(authViewModel: AuthViewModel, bleViewModel: BleViewModel) {
    val navController = rememberNavController()
    val isAuthorized by authViewModel.isAuthorized.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        mainGraph(navController, isAuthorized, authViewModel, bleViewModel)
    }
}


private fun NavGraphBuilder.mainGraph(
    navController: NavHostController,
    isAuthorized: Boolean,
    authViewModel: AuthViewModel,
    bleViewModel: BleViewModel
) {

    navigation(
        startDestination = "splashScreen",
        route = "main"
    ) {
        composable(route = "splashScreen") {
            SplashScreenRoot(
                authViewModel = authViewModel,
                onNavigateToMenu = { navController.navigate("menuScreen") },
                onNavigateToLogin = { navController.navigate("loginScreen") }
            )
        }
        composable(route = "loginScreen") {
            LoginScreenRoot(
                authViewModel = authViewModel,
                navigateToPermissionScreen = { navController.navigate("permissionScreen") },
                navigateToMenu = { navController.navigate("menuScreen") },
                navigateToSelectGroup = { navController.navigate("selectGroupScreen?fromLogin=true") }
            )
        }
        composable(route = "selectGroupScreen?fromLogin={fromLogin}") { backStackEntry ->
            val fromLogin = backStackEntry.arguments?.getString("fromLogin")?.toBooleanStrictOrNull() == true
            SelectGroupScreenRoot(
                authViewModel = authViewModel,
                navigateToPermission = { navController.navigate("permissionScreen") },
                navigateToMenu = { navController.navigate("menuScreen") }
            )
        }
        composable(route = "permissionScreen") {
            PermissionScreenRoot(
                authViewModel = authViewModel,
                onPermissionGranted = { navController.navigate("connectScreen") } // TODO: change to enableScreen
            )
        }
        composable(route = "enableScreen") {
            // TODO
        }
        composable(route = "connectScreen") {
            ConnectDeviceScreenRoot(
                bleViewModel = bleViewModel,
                onConnected = {
                    navController.navigate("menuScreen") {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                },
                onSkip = {
                    navController.navigate("menuScreen") {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(route = "menuScreen") {
            logBackstack(navController)

            MenuScreenRoot(
                authViewModel = authViewModel,
                bleViewModel = bleViewModel,
                navigateToLogin = { navController.navigate("loginScreen") },
                navigateToConnect = { navController.navigate("connectScreen") },
                navigateToDataMonitor = { sessionId ->
                    navController.navigate("dataMonitorScreen/$sessionId") },
                navigateToHistory = { navController.navigate("sessionHistoryScreen") },
                navigateToSelectGroup = { navController.navigate("selectGroupScreen") },
            )
        }
        composable(
            route = "dataMonitorScreen/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val viewModel: DataMonitorViewModel = hiltViewModel(backStackEntry)
            DataMonitorScreenRoot(
                dataMonitorViewModel = viewModel,
                navigateToMenu = {
                    navController.navigate("menuScreen") {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(route = "sessionHistoryScreen") {
            SessionHistoryScreenRoot(
                navigateToEntries = { id, isServerId ->
                    navController.navigate("entriesHistoryScreen/$id?isServerId=${isServerId}")
                },
                onBackPressed = { navController.popBackStack() }
            )
        }
        composable(
            route = "entriesHistoryScreen/{id}?isServerId={isServerId}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("isServerId") {
                    type = NavType.BoolType
                    defaultValue = true // Defaults to server ID
                }
            )
        ) { backStackEntry ->
            val viewModel: EntryHistoryViewModel = hiltViewModel(backStackEntry)
            EntryHistoryScreenRoot(
                entryHistoryViewModel = viewModel,
                onBackPressed = { navController.popBackStack() }
            )
        }
    }
}

@SuppressLint("RestrictedApi")
private fun logBackstack(navController: NavController) {
    val backstackEntries = navController.currentBackStack.value.map { it.destination.route }
    Timber.i("Backstack: $backstackEntries")
}
