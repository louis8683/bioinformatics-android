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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.louislu.pennbioinformatics.R
import com.louislu.pennbioinformatics.auth.AuthViewModel
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import timber.log.Timber

@Composable
fun LoginScreenRoot(
    authViewModel: AuthViewModel,
    navigateToPermissionScreen: () -> Unit
) {
    val isAuthorized by authViewModel.isAuthorized.collectAsState()
    val initializing by authViewModel.initializing.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // TODO

        Timber.i("activity result")
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
            navigateToPermissionScreen()
        }
    }

    LoginScreen(
        onClick = {
            Timber.d("Start auth")
            val authIntent = authViewModel.authorizationRequestIntent
            launcher.launch(authIntent)
        }
    )
}

@Composable
fun LoginScreen(
    onClick: () -> Unit,
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
                Text(text = "Welcome!", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Please login or create a new account", style = MaterialTheme.typography.labelLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onClick) {
                    Text("Login / Sign Up")
                }
            }
        }
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    LoginScreen({})
}