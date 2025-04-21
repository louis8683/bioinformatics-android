package com.louislu.pennbioinformatics.screen.menu

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.louislu.pennbioinformatics.R
import com.louislu.pennbioinformatics.auth.AuthViewModel
import com.louislu.pennbioinformatics.ble.BleViewModel
import com.louislu.pennbioinformatics.domain.model.UserInfo
import com.louislu.pennbioinformatics.screen.monitor.QuitAlertDialog
import kotlinx.coroutines.launch

@Composable
fun MenuScreenRoot(
    authViewModel: AuthViewModel,
    bleViewModel: BleViewModel,
    navigateToLogin: () -> Unit,
    navigateToConnect: () -> Unit,
    navigateToDataMonitor: (Long) -> Unit,
    navigateToHistory: () -> Unit,
    navigateToSelectGroup: () -> Unit
) {
    val menuViewModel: MenuViewModel = hiltViewModel()
    val isConnected by bleViewModel.isConnected.collectAsState()
    val userInfo by menuViewModel.userInfo.collectAsState()
    val isAuthorized by authViewModel.isAuthorized.collectAsState()
    var displayNoBluetoothSnackbar by remember { mutableStateOf(false) }
    var showQuitDialog by remember { mutableStateOf(false) }
    val pendingCount by menuViewModel.numPendingUpload.collectAsState()
    val isUploading by menuViewModel.isUploading.collectAsState()

    val context = LocalContext.current

    BackHandler {
        showQuitDialog = true
    }

    // Listen for session creation success
    LaunchedEffect(Unit) {
        menuViewModel.newSessionCreated.collect { sessionId ->
            navigateToDataMonitor(sessionId)
        }
    }

    LaunchedEffect(isAuthorized) {
        if (!isAuthorized) navigateToLogin()
    }

    MenuScreen(
        userInfo = userInfo,
        onNewSessionClicked = {
            menuViewModel.createNewSession(
                title = "",
                description = "",
                deviceName = ""     // TODO: could be pulled from BLE
            )
        },
        onHistoryClicked = { navigateToHistory() },
        onUploadClicked = { menuViewModel.syncAll() },
        onLogoutConfirmed = {
            authViewModel.logout()
            navigateToLogin()
        },
        pendingCount = pendingCount,
        isUploading = isUploading,
        isConnected = isConnected,
        onDisconnectConfirmed = { bleViewModel.disconnect() },
        onConnect = {
            if (bleViewModel.isEnabled) navigateToConnect()
            else displayNoBluetoothSnackbar = true
        },
        onChangeGroupClicked = { navigateToSelectGroup() },
        displayNoBluetoothSnackbar = displayNoBluetoothSnackbar,
        onNoBluetoothSnackbarDismissed = { displayNoBluetoothSnackbar = false },
        displayQuitAlertDialog = showQuitDialog,
        onAlertDialogOptionSelected = { quit ->
            if (quit) { (context as? Activity)?.finish() }
            showQuitDialog = false
        }
    )
}

@Composable
fun MenuScreen(
    userInfo: UserInfo?,
    onNewSessionClicked: () -> Unit,
    onHistoryClicked: () -> Unit,
    onUploadClicked: () -> Unit,
    onChangeGroupClicked: () -> Unit,
    onLogoutConfirmed: () -> Unit,
    pendingCount: Int,
    isUploading: Boolean,
    isConnected: Boolean,
    onDisconnectConfirmed: () -> Unit,
    onConnect: () -> Unit,
    displayNoBluetoothSnackbar: Boolean,
    onNoBluetoothSnackbarDismissed: () -> Unit,
    displayQuitAlertDialog: Boolean,
    onAlertDialogOptionSelected: (Boolean) -> Unit
) {
    val displayLogoutAlertDialog = remember { mutableStateOf(false) }
    val displayDisconnectAlertDialog = remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(displayNoBluetoothSnackbar) {
        if (displayNoBluetoothSnackbar) {
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Please enable Bluetooth from device settings.",
                    actionLabel = "Dismiss",
                    duration = SnackbarDuration.Indefinite
                )
                if (result == SnackbarResult.ActionPerformed) {
                    onNoBluetoothSnackbarDismissed()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                Text(text = stringResource(R.string.the_bioinformatics_app), style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = userInfo?.let { "Hi, ${userInfo.nickname}!" } ?: "Loading user info...", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
//                Text(text = "Start a session by clicking “New Session”", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    modifier = Modifier.fillMaxWidth().padding(64.dp, 0.dp),
                    enabled = isConnected && userInfo != null,
                    onClick = onNewSessionClicked) { Text("New Session")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    modifier = Modifier.fillMaxWidth().padding(64.dp, 0.dp),
                    onClick = onHistoryClicked
                ) { Text("View History") }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    modifier = Modifier.fillMaxWidth().padding(64.dp, 0.dp),
                    onClick = onUploadClicked,
                    enabled = pendingCount > 0 && !isUploading,
                ) {
                    Box(
                        modifier = Modifier.height(IntrinsicSize.Min), // Text determine height
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        if (pendingCount > 0) {
                            Text(
                                "Upload Pending ($pendingCount)",
                                modifier = Modifier.alpha(if (isUploading) 0f else 1f)
                            )
                        }
                        else {
                            Text("Upload Pending (0)")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth().padding(64.dp, 0.dp),
                    onClick = { onChangeGroupClicked() }
                ) { Text("Change Group Selection") }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth().padding(64.dp, 0.dp),
                    onClick = { displayLogoutAlertDialog.value = true }
                ) { Text("Logout") }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    modifier = Modifier.fillMaxWidth().padding(64.dp, 0.dp),
                    colors = if (isConnected) ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ) else ButtonDefaults.buttonColors(),
                    onClick = {
                        if (isConnected) displayDisconnectAlertDialog.value = true
                        else onConnect()
                    }
                ) { Text( if (isConnected) "Disconnect Device" else "Connect to Device" )}

            }
        }
    }

    when {
        displayLogoutAlertDialog.value -> {
            LogoutConfirmAlertDialog(
                onConfirm = {
                    displayLogoutAlertDialog.value = false
                    onLogoutConfirmed()
                },
                onDismiss = { displayLogoutAlertDialog.value = false }
            )
        }

        displayQuitAlertDialog -> {
            QuitAlertDialog(
                onDismiss = { onAlertDialogOptionSelected(false) },
                onConfirmClicked = { onAlertDialogOptionSelected(true) },
                onDismissClicked = { onAlertDialogOptionSelected(false) },
            )
        }

        displayDisconnectAlertDialog.value -> {
            DisconnectConfirmAlertDialog(
                onConfirm = {
                    displayDisconnectAlertDialog.value = false
                    if (isConnected) onDisconnectConfirmed()
                },
                onDismiss = {
                    displayDisconnectAlertDialog.value = false
                }
            )
        }
    }
}

@Composable
private fun LogoutConfirmAlertDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        title = { Text("Logout") },
        text = { Text("Are you sure you want to logout? You must be logged in to record data. Logging in will require internet connection.") },
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onConfirm) { Text("Logout") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DisconnectConfirmAlertDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        title = { Text("Disconnect") },
        text = { Text("Are you sure you want to disconnect?") },
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onConfirm) { Text("Disconnect") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Preview
@Composable
fun MenuScreenPreview() {
    val userInfo = UserInfo("","", "","","","","","John Doe")
    MenuScreen(userInfo, {}, {}, {}, {}, {}, 10, false, false, {}, {}, false, {}, false, {})
}

@Preview
@Composable
fun MenuScreenPreviewConnected() {
    val userInfo = UserInfo("","", "","","","","","John Doe")
    MenuScreen(userInfo, {}, {}, {}, {}, {}, 10, false, true, {}, {}, false, {}, false, {})
}

@Preview
@Composable
fun MenuScreenPreviewUploading() {
    val userInfo = UserInfo("","", "","","","","","John Doe")
    MenuScreen(userInfo, {}, {}, {}, {}, {}, 10, true, false, {}, {}, false, {}, false, {})
}

@Preview
@Composable
fun MenuScreenPreviewNoPending() {
    val userInfo = UserInfo("","", "","","","","","John Doe")
    MenuScreen(userInfo, {}, {}, {}, {}, {}, 0, false, false, {}, {}, false, {}, false, {})
}