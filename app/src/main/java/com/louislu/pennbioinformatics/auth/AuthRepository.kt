package com.louislu.pennbioinformatics.auth

import android.content.Intent
import kotlinx.coroutines.flow.StateFlow
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import org.json.JSONObject

interface AuthRepository {
    fun isAuthorized(): StateFlow<Boolean>
    fun initializeAppAuth()
    fun getAuthorizationRequestIntent(): Intent

    fun onAuthorizationResult(response: AuthorizationResponse?, ex: AuthorizationException?)

    suspend fun getUserInfo(): JSONObject

    fun logout()

    // TODO: getUserId()

//    suspend fun initializeAuth()
//    suspend fun createAuthRequest(): AuthorizationRequest
//    suspend fun warmUpBrowser()
//    suspend fun exchangeAuthorizationCode(response: AuthorizationResponse)
//    fun performTokenRequest(request: TokenRequest, callback: AuthorizationService.TokenResponseCallback)
//    fun isUserAuthorized(): Boolean
}