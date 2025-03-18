package com.louislu.pennbioinformatics

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.louislu.pennbioinformatics.auth.AuthViewModel
import com.louislu.pennbioinformatics.ble.BleViewModel
import com.louislu.pennbioinformatics.ui.theme.PennBioinformaticsTheme
import dagger.hilt.android.AndroidEntryPoint
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val RC_AUTH = 100
        private const val EXTRA_FAILED: String = "failed"
    }

    // Defined here to survive configuration changes
    private val authViewModel: AuthViewModel by viewModels()
    private val bleViewModel: BleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize AppAuth
        if (intent.getBooleanExtra(EXTRA_FAILED, false)) {
            // TODO("show user that the authorization has been cancelled")
            Timber.i("Auth cancelled")
            authViewModel.onAuthCancelled()
        }

        enableEdgeToEdge()
        setContent {
            PennBioinformaticsTheme {
                NavigationRoot(
                    authViewModel,
                    bleViewModel,
                    onLoginClicked = {
                        Timber.d("Start auth")
                        val authIntent = authViewModel.authorizationRequestIntent
                        startActivityForResult(authIntent, RC_AUTH)
                    }
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Timber.i("onActivityResult()")
        if (resultCode == RESULT_CANCELED) {
            Timber.i("Auth cancelled")
        } else {
            data?.let {
                val response = AuthorizationResponse.fromIntent(data)
                val ex = AuthorizationException.fromIntent(data)

                authViewModel.onAuthorizationResult(response, ex)
            }
        }
    }
}