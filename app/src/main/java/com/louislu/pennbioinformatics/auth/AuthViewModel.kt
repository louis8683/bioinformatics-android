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
import com.louislu.pennbioinformatics.domain.model.UserInfo
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
import retrofit2.HttpException
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

    private val _userInfo = MutableStateFlow<UserInfo?>(null)
    val userInfo: StateFlow<UserInfo?> = _userInfo

    private val _initializing = MutableStateFlow(true)
    val initializing: StateFlow<Boolean> = _initializing

    init {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    authRepository.initializeAppAuth()
                    try {
                        authRepository.getAccessToken()
                    } catch (e: AuthError.Network.NoAccessToken) {
                        Timber.i("No access token, redo login")
                    } catch (e: Exception) {
                        Timber.i("Other exception: $e")
                    }
                }
            } catch (e: Exception) {
                Timber.i("Error during initialization of AppAuth: $e")
            }
            _initializing.value = false
        }
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
            _userInfo.value = authRepository.getUserInfo()
            Timber.i("Get User Info Result: ${userInfo.value}")
        }
    }

    fun updateUserInfo(schoolName:String, className: String, groupName: String) {
        viewModelScope.launch {
            try {
                authRepository.updateUserInfo(schoolName, className, groupName)
                getUserInfo()
            } catch (e: Exception) {
                Timber.i("Exception: $e")
                // TODO: gracefully handle
            }
        }
    }

    fun logout() { authRepository.logout() }
}