package com.louislu.pennbioinformatics.auth

import net.openid.appauth.AuthorizationException

sealed class AuthError(message: String? = null, cause: Throwable? = null) : Throwable(message, cause) {

    /** Errors related to network authentication failures */
    sealed class Network(message: String? = null, cause: Throwable? = null) : AuthError(message, cause) {
        class AuthorizationCodeExchangedFailed(authException: AuthorizationException?): Network(
    "Authorization Code exchange failed"
            + (if ((authException != null)) authException.error else ""))
        class RetrieveDiscoveryFailed: Network()
        class TokenRefreshFailed: Network()
        class FetchUserInfoNetworkException: Network()
        class JSONParseException: Network()
        class NoAccessToken: Network()
    }

    /** Errors related to local authentication issues */
    sealed class Local(message: String? = null, cause: Throwable? = null) : AuthError(message, cause) {
        class InvalidConfiguration: Local("Invalid configuration")
        class MissingDiscoveryUriOrExplicitEndpoints: Local("Require discovery uri or explicit endpoints")
        class MissingClientId: Local("Client ID not configured")
        class AuthServiceNotInitialized: Local("AuthService not initialized")
    }

    /** Custom error with details (e.g., API error codes) */
//    data class Custom(val code: Int, val description: String) : AuthError("Custom auth error: $description")
}