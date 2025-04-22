package com.louislu.pennbioinformatics.screen.permission

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.louislu.pennbioinformatics.R
import com.louislu.pennbioinformatics.auth.AuthViewModel
import com.louislu.pennbioinformatics.requiredPermissions
import timber.log.Timber

@Composable
fun PermissionScreenRoot(
    authViewModel: AuthViewModel,
    onPermissionGranted: () -> Unit
) {
    var permissionsState by remember { mutableStateOf(false) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Timber.i("Permissions: ${permissions.keys}, Status: ${permissions.values}")
        permissionsState = permissions.all { it.value }
    }

    val userInfo by authViewModel.userInfo.collectAsState()

    LaunchedEffect(Unit) {
        permissionsLauncher.launch(requiredPermissions)
    }

    LaunchedEffect(permissionsState) {
        Timber.i("Permission state updated to: $permissionsState")
        if (permissionsState) onPermissionGranted()
    }

    // UI for permission retry
    PermissionScreen(
        username = userInfo?.nickname ?: "",
        onClick = {
            permissionsLauncher.launch(requiredPermissions)
        }
    )
}

@Composable
fun PermissionScreen(
    username: String,
    onClick: () -> Unit
) {

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
                Text(text = if (username.isNotEmpty()) "Hi, $username!" else "Hi!", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Please grant permission for Bluetooth and Location", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onClick) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

@Preview
@Composable
private fun PermissionScreenPreview() {
    PermissionScreen("John Doe", {})
}