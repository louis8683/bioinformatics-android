package com.louislu.pennbioinformatics.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.louislu.pennbioinformatics.R
import com.louislu.pennbioinformatics.auth.AuthViewModel
import com.louislu.pennbioinformatics.ble.BleViewModel

@Composable
fun MenuScreenRoot(
    authViewModel: AuthViewModel,
    bleViewModel: BleViewModel,
    navigateToLogin: () -> Unit,
    navigateToConnect: () -> Unit,
    navigateToDataMonitor: () -> Unit
) {
    val isConnected by bleViewModel.isConnected.collectAsState()

    MenuScreen(
        username = "(placeholder: TODO",
        onNewSessionClicked = { /*TODO*/ },
        onHistoryClicked = { /*TODO*/ },
        onUploadClicked = { /*TODO*/ },
        onLogoutConfirmed = {
            authViewModel.logout()
            navigateToLogin()
        },
        pendingCount = -1,
        isUploading = false,
        isConnected = isConnected,
        onDisconnect = {
            bleViewModel.disconnect()
        },
        onConnect = {
            navigateToConnect()
        }
    )
}

@Composable
fun MenuScreen(
    username: String,
    onNewSessionClicked: () -> Unit,
    onHistoryClicked: () -> Unit,
    onUploadClicked: () -> Unit,
    onLogoutConfirmed: () -> Unit,
    pendingCount: Int,
    isUploading: Boolean,
    isConnected: Boolean,
    onDisconnect: () -> Unit,
    onConnect: () -> Unit
) {
    val openAlertDialog = remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
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
                Text(text = "Hi, $username!", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Start a session by clicking “New Session”", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    enabled = isConnected,
                    onClick = onNewSessionClicked) { Text("New Session")
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onHistoryClicked) { Text("View History") }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
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
                OutlinedButton(onClick = { openAlertDialog.value = true }) { Text("Logout") }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    colors = if (isConnected) ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ) else ButtonDefaults.buttonColors(),
                    onClick = if (isConnected) onDisconnect else onConnect
                ) { Text( if (isConnected) "Disconnect Device" else "Connect to Device" )}

            }
        }
    }

    when {
        openAlertDialog.value -> {
            LogoutConfirmAlertDialog(
                onConfirm = onLogoutConfirmed,
                onDismiss = { openAlertDialog.value = false }
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
        text = { Text("You must be logged in to record data. Logging in will require internet connection.") },
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onConfirm) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Preview
@Composable
fun MenuScreenPreview() {
    MenuScreen("John Doe", {}, {}, {}, {}, 10, false, false, {}, {})
}

@Preview
@Composable
fun MenuScreenPreviewUploading() {
    MenuScreen("John Doe", {}, {}, {}, {}, 10, true, false, {}, {})
}

@Preview
@Composable
fun MenuScreenPreviewNoPending() {
    MenuScreen("John Doe", {}, {}, {}, {}, 0, false, false, {}, {})
}