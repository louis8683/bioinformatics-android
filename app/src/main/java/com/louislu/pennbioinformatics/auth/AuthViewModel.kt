package com.louislu.pennbioinformatics.auth

import android.app.Application
import android.content.Intent
import android.text.TextUtils
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.louislu.logintester.appauthhelper.AuthStateManager
import com.louislu.logintester.appauthhelper.Configuration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationService.TokenResponseCallback
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ClientAuthentication
import net.openid.appauth.ClientAuthentication.UnsupportedAuthenticationMethod
import net.openid.appauth.ClientSecretBasic
import net.openid.appauth.RegistrationRequest
import net.openid.appauth.RegistrationResponse
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import net.openid.appauth.browser.AnyBrowserMatcher
import net.openid.appauth.browser.BrowserMatcher
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
): ViewModel() {

    private val _isAuthorized = authRepository.isAuthorized()
    val isAuthorized: StateFlow<Boolean> = _isAuthorized
    val authorizationRequestIntent: Intent by lazy {
        authRepository.getAuthorizationRequestIntent()
    }
    private val _authCancelledFlow = MutableSharedFlow<Unit>()
    val authCancelledFlow: SharedFlow<Unit> = _authCancelledFlow

    init {
        authRepository.initializeAppAuth()
    }

    fun onAuthCancelled() {
        viewModelScope.launch {
            _authCancelledFlow.emit(Unit)
        }
    }

    fun onAuthorizationResult(response: AuthorizationResponse?, ex: AuthorizationException?) {
        authRepository.onAuthorizationResult(response, ex)
    }

    fun getUserInfo() {
        viewModelScope.launch {
            val userInfo = authRepository.getUserInfo()
            Timber.i("Get User Info Result: $userInfo")
        }
    }

    fun logout() { authRepository.logout() }
}