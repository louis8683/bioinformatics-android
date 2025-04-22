package com.louislu.pennbioinformatics.screen.login

import android.app.Activity.RESULT_CANCELED
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.louislu.pennbioinformatics.R
import com.louislu.pennbioinformatics.auth.AuthViewModel
import com.louislu.pennbioinformatics.hasInternetConnection
import com.louislu.pennbioinformatics.hasPermissions
import com.louislu.pennbioinformatics.requiredPermissions
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import timber.log.Timber

@Composable
fun LoginScreenRoot(
    authViewModel: AuthViewModel,
    navigateToPermissionScreen: () -> Unit,
    navigateToSelectGroup: () -> Unit,
    navigateToMenu: () -> Unit
) {
    val isAuthorized by authViewModel.isAuthorized.collectAsState()
    val initializing by authViewModel.initializing.collectAsState()
    var isLoggingIn by remember { mutableStateOf(false) }
    var displayNoInternetSnackbar by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Timber.i("activity result")
        isLoggingIn = false
        if (result.resultCode == RESULT_CANCELED) {
            Timber.d("Auth cancelled")
        } else {
            result.data?.let {
                val response = AuthorizationResponse.fromIntent(it)
                val ex = AuthorizationException.fromIntent(it)

                authViewModel.onAuthorizationResult(response, ex)
            }
        }
    }

    Timber.i("isAuthorized: $isAuthorized")

    LaunchedEffect(initializing, isAuthorized) {
        Timber.i("initializing: $initializing, isAuthorized: $isAuthorized")
        if (!initializing && isAuthorized) {
            Timber.i("Initialization completed. Is authorized? $isAuthorized")

            val userInfo = authViewModel.userInfo.first()
            if (hasPermissions(context, requiredPermissions) && userInfo != null && userInfo.schoolName != null && userInfo.className != null && userInfo.groupName != null) {
                navigateToMenu()
            }
            else if (hasPermissions(context, requiredPermissions)) {
                navigateToSelectGroup()
            }
            else {
                navigateToPermissionScreen()
            }
        }
    }

    LoginScreen(
        isLoggingIn = isLoggingIn,
        displayNoInternetSnackbar = displayNoInternetSnackbar,
        onNoInternetSnackbarDismissed = {
            displayNoInternetSnackbar = false
        },
        onClick = {
            if (hasInternetConnection(context)) {
                isLoggingIn = true
                Timber.d("Start auth")
                val authIntent = authViewModel.authorizationRequestIntent
                launcher.launch(authIntent)
            }
            else {
                displayNoInternetSnackbar = true
            }
        },
    )
}

@Composable
fun LoginScreen(
    isLoggingIn: Boolean,
    displayNoInternetSnackbar: Boolean,
    onNoInternetSnackbarDismissed: () -> Unit,
    onClick: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(displayNoInternetSnackbar) {
        if (displayNoInternetSnackbar) {
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "No Internet Connection.",
                    actionLabel = "Dismiss",
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    onNoInternetSnackbarDismissed()
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
                Text(text = "Welcome!", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Please login or create a new account", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onClick, enabled = !isLoggingIn) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Login / Sign up", color = if (isLoggingIn) Color.Transparent else Color.Unspecified)

                        if (isLoggingIn) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier
                                    .size(20.dp) // Adjust size to match text height
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    widthDp = 320,
    heightDp = 568,
    name = "Small Screen Preview"
)
@Composable
private fun LoginScreenPreview() {
    LoginScreen(false, false, {}, {})
}

@Preview(
    showBackground = true,
    widthDp = 320,
    heightDp = 568,
    name = "Small Screen Preview"
)
@Composable
private fun LoginScreenLoadingPreview() {
    LoginScreen(true, false, {}, {})
}