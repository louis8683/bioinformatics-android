package com.louislu.pennbioinformatics.auth

import android.content.Intent
import com.louislu.pennbioinformatics.domain.model.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import org.json.JSONObject

interface AuthRepository {
    fun isAuthorized(): StateFlow<Boolean>
    suspend fun initializeAppAuth()
    fun getAuthorizationRequestIntent(): Intent

    fun onAuthorizationResult(response: AuthorizationResponse?, ex: AuthorizationException?)

    suspend fun getUserInfo(): UserInfo?

    suspend fun updateUserInfo(schoolName: String, className: String, groupName: String): Result<Unit>

    suspend fun getAccessToken(): Result<String>

    fun logout()

    // TODO: getUserId()

//    suspend fun initializeAuth()
//    suspend fun createAuthRequest(): AuthorizationRequest
//    suspend fun warmUpBrowser()
//    suspend fun exchangeAuthorizationCode(response: AuthorizationResponse)
//    fun performTokenRequest(request: TokenRequest, callback: AuthorizationService.TokenResponseCallback)
//    fun isUserAuthorized(): Boolean
}