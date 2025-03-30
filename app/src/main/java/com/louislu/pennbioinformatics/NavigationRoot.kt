package com.louislu.pennbioinformatics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.louislu.pennbioinformatics.screen.PermissionScreenRoot
import com.louislu.pennbioinformatics.screen.group.SelectGroupScreenRoot
import com.louislu.pennbioinformatics.screen.history.SessionHistoryScreenRoot

@Composable
fun NavigationRoot(authViewModel: AuthViewModel, bleViewModel: BleViewModel, onLoginClicked: () -> Unit) {
    val navController = rememberNavController()
    val isAuthorized by authViewModel.isAuthorized.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "main"
    ) {
        mainGraph(navController, onLoginClicked, isAuthorized, authViewModel, bleViewModel)
    }
}


private fun NavGraphBuilder.mainGraph(
    navController: NavHostController,
    onLoginClicked: () -> Unit,
    isAuthorized: Boolean,
    authViewModel: AuthViewModel,
    bleViewModel: BleViewModel
) {

    navigation(
        startDestination = "loginScreen",
        route = "main"
    ) {
        composable(route = "loginScreen") {
            LoginScreenRoot(
                authViewModel = authViewModel,
                onLoginClicked = onLoginClicked,
                navigateToPermissionScreen = { navController.navigate("selectGroupScreen?fromLogin=true") }
            )
        }
        composable(route = "selectGroupScreen?fromLogin={fromLogin}") { backStackEntry ->
            val fromLogin = backStackEntry.arguments?.getString("fromLogin")?.toBooleanStrictOrNull() == true
            SelectGroupScreenRoot(
                authViewModel = authViewModel,
                navigateToPermission = { navController.navigate("permissionScreen") },
                fromLogin = fromLogin
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
                onConnected = { navController.navigate("menuScreen") },
                onSkip = { navController.navigate("menuScreen") }
            )
        }
        composable(route = "menuScreen") {
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
                navigateToMenu = { navController.navigate("menuScreen") }
            )
        }

        composable(route = "sessionHistoryScreen") {
            SessionHistoryScreenRoot(
                navigateToEntries = { serverId -> navController.navigate("entriesHistoryScreen/$serverId") },
                onBackPressed = { navController.popBackStack() }
            )
        }
        composable(
            route = "entriesHistoryScreen/{sessionServerId}",
            arguments = listOf(navArgument("sessionServerId") { type = NavType.LongType })
        ) { backStackEntry ->
            val viewModel: EntryHistoryViewModel = hiltViewModel(backStackEntry)
            EntryHistoryScreenRoot (
                entryHistoryViewModel = viewModel,
                onBackPressed = { navController.popBackStack() }
            )
        }
    }
}