package com.louislu.pennbioinformatics

import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.louislu.pennbioinformatics.auth.AuthViewModel
import com.louislu.pennbioinformatics.ble.BleViewModel
import com.louislu.pennbioinformatics.screen.ConnectDeviceScreenRoot
import com.louislu.pennbioinformatics.screen.DataMonitorScreenRoot
import com.louislu.pennbioinformatics.screen.LoginScreen
import com.louislu.pennbioinformatics.screen.LoginScreenRoot
import com.louislu.pennbioinformatics.screen.MenuScreen
import com.louislu.pennbioinformatics.screen.MenuScreenRoot
import com.louislu.pennbioinformatics.screen.PermissionScreen
import com.louislu.pennbioinformatics.screen.PermissionScreenRoot

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
                navigateToPermissionScreen = { navController.navigate("permissionScreen") }
            )
        }
        composable(route = "permissionScreen") {
            PermissionScreenRoot(
                authViewModel = authViewModel,
                onPermissionGranted = { navController.navigate("connectScreen") }
            )
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
                navigateToDataMonitor = { }
            )
        }
        composable(route = "dataMonitorScreen") {
            DataMonitorScreenRoot(
                bleViewModel = bleViewModel
            )
        }
    }
}