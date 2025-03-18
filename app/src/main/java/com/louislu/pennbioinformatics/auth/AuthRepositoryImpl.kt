package com.louislu.pennbioinformatics.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import androidx.browser.customtabs.CustomTabsIntent
import com.louislu.logintester.appauthhelper.AuthStateManager
import com.louislu.logintester.appauthhelper.Configuration
import com.louislu.pennbioinformatics.safeLet
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.AuthorizationServiceDiscovery
import net.openid.appauth.ClientAuthentication
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.TokenRequest
import net.openid.appauth.TokenResponse
import net.openid.appauth.browser.AnyBrowserMatcher
import net.openid.appauth.browser.BrowserMatcher
import okio.buffer
import okio.source
import okio.use
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.net.HttpURLConnection
import java.nio.charset.StandardCharsets
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

class AuthRepositoryImpl(
    @ApplicationContext private val context: Context
): AuthRepository {

    /** Private Properties **/

    private val mAuthStateManager = AuthStateManager.getInstance(context)
    private val mConfiguration = Configuration.getInstance(context)
    private var mAuthService: AuthorizationService? = null
    private val mAuthRequest = AtomicReference<AuthorizationRequest>()
    private val mAuthIntent = AtomicReference<CustomTabsIntent>()
    private val mClientId = AtomicReference<String>()
    private var mAuthIntentLatch = CountDownLatch(1)

    private val _isAuthorized = MutableStateFlow(mAuthStateManager.current.isAuthorized)

    /** Initialization **/

    init {
        // TODO: Maybe we can inject the AuthStateManager instead of creating one here? Think about
        //  scope and lifecycles when approaching it

        if (mConfiguration.hasConfigurationChanged()) {
            // discard any existing authorization state due to the change of configuration
            Timber.i("Configuration change detected, discarding old state")
            mAuthStateManager.replace(AuthState())
            mConfiguration.acceptConfiguration()
        }

        if (!mConfiguration.isValid) {
            Timber.e("Configuration error: ${mConfiguration.configurationError}")
            throw AuthError.Local.InvalidConfiguration() // TODO: send error to flow instead of throwing
        }
    }

    /** Override Functions **/

    override fun isAuthorized(): StateFlow<Boolean> { return _isAuthorized }

    override fun initializeAppAuth() {
        Timber.i("Initializing AppAuth")
        recreateAuthorizationService()

        if (mAuthStateManager.current.getAuthorizationServiceConfiguration() != null) {
            // configuration is already created, skip to client initialization
            Timber.i("auth config already established")
            initializeClient()
            createAuthRequest()
            warmUpBrowser()
        }

        // if we are not using discovery, build the authorization service configuration directly
        // from the static configuration values.
        mConfiguration.discoveryUri?.let { discoveryUri ->
            Timber.i("Retrieving OpenID discovery doc")
            AuthorizationServiceConfiguration.fetchFromUrl(
                discoveryUri,
                { config, ex ->

                    config?.let {
                        Timber.i("Discovery document retrieved")
                        mAuthStateManager.replace(AuthState(config))
                        initializeClient()
                        createAuthRequest()
                        warmUpBrowser()
                    } ?: throw AuthError.Network.RetrieveDiscoveryFailed() // TODO: elegant handling
                },
                mConfiguration.connectionBuilder
            )
        } ?: {
            Timber.i( "Not using discovery, creating auth config from res/raw/auth_config.json")
            val config = safeLet(
                mConfiguration.authEndpointUri,
                mConfiguration.tokenEndpointUri,
                mConfiguration.registrationEndpointUri,
                mConfiguration.endSessionEndpoint
            ) { auth, token, registration, endSession ->
                AuthorizationServiceConfiguration(auth, token, registration, endSession)
            } ?: throw AuthError.Local.MissingDiscoveryUriOrExplicitEndpoints() // TODO: handle this elegantly

            mAuthStateManager.replace(AuthState(config))
            initializeClient()
            createAuthRequest()
            warmUpBrowser()
        }
    }

    override fun getAuthorizationRequestIntent(): Intent {
        try {
            Timber.d("Latch Count: " + mAuthIntentLatch.count)
            mAuthIntentLatch.await()
        } catch (ex: InterruptedException) {
            Timber.w("Interrupted while waiting for auth intent")
        }

        return mAuthService?.getAuthorizationRequestIntent(
            mAuthRequest.get(),
            mAuthIntent.get()
        ) ?: throw AuthError.Local.AuthServiceNotInitialized()
    }

    override fun onAuthorizationResult(
        response: AuthorizationResponse?,
        ex: AuthorizationException?
    ) {
        Timber.i("Received response: $response")
        Timber.i("Received exception: $ex")

        if (response != null || ex != null) {
            mAuthStateManager.updateAfterAuthorization(response, ex)
        }

        if (response?.authorizationCode != null) {
            // authorization code exchange is required
            mAuthStateManager.updateAfterAuthorization(response, ex)
            exchangeAuthorizationCode(response)
        } else if (ex != null) {
            Timber.i("Authorization flow failed: " + ex.message)
            // TODO("Display: Authorization flow failed: " + ex.message)
        } else {
            Timber.i("No authorization state retained - reauthorization required")
            // TODO("Display: No authorization state retained - reauthorization required")
        }
    }

    override suspend fun getUserInfo(): JSONObject {
        var accessToken: String? = null
        var ex: AuthorizationException? = null

        mAuthService?.let {
            mAuthStateManager.current.performActionWithFreshTokens(it) {
                access, _, e ->
                accessToken = access
                ex = e
            }
        } ?: throw AuthError.Local.AuthServiceNotInitialized()

        return fetchUserInfo(accessToken, ex)
    }

    override fun logout() {

        // discard the authorization and token state, but retain the configuration and
        // dynamic client registration (if applicable), to save from retrieving them again.
        val currentState: AuthState = mAuthStateManager.current
        val clearedState = currentState
            .authorizationServiceConfiguration?.let { AuthState(it) } ?: AuthState()
        currentState.lastRegistrationResponse?.let {
            clearedState.update(currentState.lastRegistrationResponse) }

        mAuthStateManager.replace(clearedState)

        _isAuthorized.value = mAuthStateManager.current.isAuthorized
    }

    /** Private Functions **/

    private fun recreateAuthorizationService() {
        Timber.i("Discarding existing AuthService instance")
        mAuthService?.dispose()

        mAuthService = createAuthorizationService()
        mAuthRequest.set(null)
        mAuthIntent.set(null)
    }

    private fun createAuthorizationService(): AuthorizationService {
        Timber.i("Creating authorization service")
        val builder = AppAuthConfiguration.Builder()
        val mBrowserMatcher: BrowserMatcher = AnyBrowserMatcher.INSTANCE
        builder.setBrowserMatcher(mBrowserMatcher)

        builder.setConnectionBuilder(mConfiguration.connectionBuilder)
        return AuthorizationService(context, builder.build())
    }

    private fun initializeClient() {

        mConfiguration.clientId?.let {
            Timber.i("Using static client ID: " + mConfiguration.clientId)

            // use a statically configured client ID
            mClientId.set(requireNotNull(mConfiguration.clientId))
        } ?: throw AuthError.Local.MissingClientId()

        // NOTE: Dynamic Client Registration (DCR) is disabled
    }

    private fun createAuthRequest(loginHint: String? = null) {
        Timber.i("Creating auth request for login hint: $loginHint")
        val authRequestBuilder = AuthorizationRequest.Builder(
            requireNotNull(mAuthStateManager.current.authorizationServiceConfiguration),
            mClientId.get(),
            ResponseTypeValues.CODE,
            mConfiguration.redirectUri
        )
            .setScope(mConfiguration.scope)

        if (!TextUtils.isEmpty(loginHint)) {
            authRequestBuilder.setLoginHint(loginHint)
        }

        mAuthRequest.set(authRequestBuilder.build())
    }

    private fun warmUpBrowser() {
        mAuthIntentLatch = CountDownLatch(1)
        Timber.i("Warming up browser instance for auth request")
        val intentBuilder =
            mAuthService?.createCustomTabsIntentBuilder(mAuthRequest.get().toUri())
        if (intentBuilder != null) {
            mAuthIntent.set(intentBuilder.build())
        }
        mAuthIntentLatch.countDown()
    }

    // After Authorization

    private fun exchangeAuthorizationCode(authorizationResponse: AuthorizationResponse) {
        Timber.i("Exchanging authorization code")
        // TODO("Display: Exchanging authorization code")
        performTokenRequest(
            authorizationResponse.createTokenExchangeRequest()
        ) { tokenResponse, authException ->
            this.handleCodeExchangeResponse(
                tokenResponse,
                authException
            )
        }
    }

    private fun handleCodeExchangeResponse(
        tokenResponse: TokenResponse?,
        authException: AuthorizationException?
    ) {
        mAuthStateManager.updateAfterTokenResponse(tokenResponse, authException)
        if (!mAuthStateManager.current.isAuthorized) {
            throw AuthError.Network.AuthorizationCodeExchangedFailed(authException)
        }

        _isAuthorized.value = mAuthStateManager.current.isAuthorized
        Timber.i("Auth state updated: isAuthorized = ${mAuthStateManager.current.isAuthorized}")
    }

    private fun performTokenRequest(
        request: TokenRequest,
        callback: AuthorizationService.TokenResponseCallback
    ) {
        val clientAuthentication: ClientAuthentication
        try {
            clientAuthentication = mAuthStateManager.current.clientAuthentication
        } catch (ex: ClientAuthentication.UnsupportedAuthenticationMethod) {
            Timber.d("Token request cannot be made, client authentication for the token "
                    + "endpoint could not be constructed (%s)", ex)
            // TODO("Display: Client authentication method is unsupported")
            return
        }

        mAuthService?.performTokenRequest(
            request,
            clientAuthentication,
            callback
        ) ?: throw AuthError.Local.AuthServiceNotInitialized() // TODO: elegant handling
    }

    // User Info

    private suspend fun fetchUserInfo(accessToken: String?, ex: AuthorizationException?): JSONObject {
        if (ex != null) {
            Timber.e("Token refresh failed when fetching user info: ${ex.message}")
            throw AuthError.Network.TokenRefreshFailed()
        }

        val discovery: AuthorizationServiceDiscovery? =
            mAuthStateManager.current.getAuthorizationServiceConfiguration()?.discoveryDoc

        val userInfoEndpoint = mConfiguration.userInfoEndpointUri
            ?: discovery?.let { Uri.parse(it.userinfoEndpoint.toString()) }
            ?: throw AuthError.Local.MissingDiscoveryUriOrExplicitEndpoints()

        Timber.d("Endpoint: " + AuthorizationServiceConfiguration.WELL_KNOWN_PATH)
        Timber.d("User info endpoint: $userInfoEndpoint")

        return withContext(Dispatchers.IO) {
            try {
                Timber.d("Sending request with the Bearer token as: $accessToken")

                val conn: HttpURLConnection = mConfiguration
                    .connectionBuilder
                    .openConnection(userInfoEndpoint)
                conn.setRequestProperty("Authorization", "Bearer $accessToken")
                conn.instanceFollowRedirects = false
                val response = conn.inputStream.source().buffer().use {
                    it.readString(StandardCharsets.UTF_8)
                }


                Timber.d("User info response: $response")

                JSONObject(response)
            } catch (ioEx: IOException) {
                Timber.e("Network error when querying userinfo endpoint", ioEx)
                throw AuthError.Network.FetchUserInfoNetworkException()
            } catch (jsonEx: JSONException) {
                Timber.e("Failed to parse userinfo response")
                throw AuthError.Network.JSONParseException()
            }
        }
    }
}