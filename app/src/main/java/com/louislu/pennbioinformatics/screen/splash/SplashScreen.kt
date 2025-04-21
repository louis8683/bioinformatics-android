package com.louislu.pennbioinformatics.screen.splash

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.louislu.pennbioinformatics.R
import com.louislu.pennbioinformatics.auth.AuthViewModel
import kotlinx.coroutines.delay
import timber.log.Timber

private const val DELAY_TIME = 1000

@Composable
fun SplashScreenRoot(
    authViewModel: AuthViewModel,
    onNavigateToMenu: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val isAuthorized by authViewModel.isAuthorized.collectAsState()
    val initializing by authViewModel.initializing.collectAsState()
    var splashAnimating by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(DELAY_TIME.toLong())
        splashAnimating = false
    }

    LaunchedEffect(initializing, isAuthorized, splashAnimating) {
        Timber.i("initializing: $initializing, isAuthorized: $isAuthorized")
        if (!splashAnimating && !initializing) {
            if (isAuthorized) {
                Timber.i("Initialization completed. Authorized.")
                onNavigateToMenu() // TODO: should we check permission once more? should we check everything here?
            } else {
                Timber.d("Initialization completed. Not authorized.")
                onNavigateToLogin()
            }
        }
    }

    SplashScreen(isInitializing = initializing)
}

@Composable
fun SplashScreen(
    isInitializing: Boolean,
    modifier: Modifier = Modifier,
) {
    var showSpinner by remember { mutableStateOf(false) }

    // Controls the fade-in animation
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = DELAY_TIME,
            easing = LinearOutSlowInEasing
        ),
        label = "fadeIn",
    )

    // After 1 second, allow spinner to show if still initializing
    LaunchedEffect(Unit) {
        delay(1000L)
        showSpinner = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo with fade-in
            Image(
                painter = painterResource(id = R.drawable.ic_logo), // replace with your logo
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer(alpha = alpha)
            )

            // Show spinner only if fade-in is done AND init still in progress
            if (showSpinner && isInitializing) {
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator()
            }
        }
    }
}