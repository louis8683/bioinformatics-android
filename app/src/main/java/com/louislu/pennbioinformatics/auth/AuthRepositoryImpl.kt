package com.louislu.pennbioinformatics.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import androidx.browser.customtabs.CustomTabsIntent
import com.louislu.logintester.appauthhelper.AuthStateManager
import com.louislu.logintester.appauthhelper.Configuration
import com.louislu.pennbioinformatics.domain.model.UserInfo
import com.louislu.pennbioinformatics.safeLet
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
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
import net.openid.appauth.connectivity.ConnectionBuilder
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
import kotlin.coroutines.resumeWithException

class AuthRepositoryImpl(
    private val userService: UserService,
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

    override fun isAuthorized(): StateFlow<Boolean> {
        _isAuthorized.value = mAuthStateManager.current.isAuthorized // TODO: is there a better way to reactively update the token?
        return _isAuthorized
    }

    override suspend fun initializeAppAuth() {
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
            try {
                // If we already have the config, we don't update it
                if (mAuthStateManager.current.authorizationServiceConfiguration == null) {
                    val config = fetchDiscoveryDocument(discoveryUri, mConfiguration.connectionBuilder)
                    Timber.i("Discovery document retrieved")
                    mAuthStateManager.replace(AuthState(config))
                }
                initializeClient()
                createAuthRequest()
                warmUpBrowser()
            } catch (e: Exception) {
                Timber.e("Exception during discovery document retrieval: $e")
                throw e
            }
        } ?: throw IllegalStateException("Must have discovery document endpoint in config")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun fetchDiscoveryDocument(
        discoveryUri: Uri,
        connectionBuilder: ConnectionBuilder
    ): AuthorizationServiceConfiguration = suspendCancellableCoroutine { continuation ->
        AuthorizationServiceConfiguration.fetchFromUrl(
            discoveryUri,
            { config, ex ->
                if (ex != null) {
                    continuation.resumeWithException(ex)
                } else if (config != null) {
                    continuation.resume(config){}
                } else {
                    continuation.resumeWithException(AuthError.Network.RetrieveDiscoveryFailed())
                }
            },
            connectionBuilder
        )
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

    override suspend fun getUserInfo(): UserInfo? {
        val json = fetchUserInfo()
        return json?.let {
            val info = UserInfo(
                userId = json.getString("sub"),
                className = json.optString("custom:class_name", null),
                groupName = json.optString("custom:group_name", null),
                schoolName = json.optString("custom:school_name", null),
                email = json.optString("email", null),
                familyName = json.getString("family_name"),
                givenName = json.getString("given_name"),
                nickname = json.getString("nickname")
            )
            saveUserInfo(info)
            info
        } ?: loadUserInfoFromDataStore()
    }

    override suspend fun updateUserInfo(
        schoolName: String,
        className: String,
        groupName: String
    ): Result<Unit> = runCatching {

        getAccessToken().onSuccess { token ->
            val request = UpdateUserInfoRequest(
                access_token = token,
                group_name = groupName,
                class_name = className,
                school_name = schoolName
            )
            userService.updateUserInfo("Bearer $token", request)
            getUserInfo()?.let {
                saveUserInfo(
                    it.copy(
                        groupName = groupName,
                        className = className,
                        schoolName = schoolName
                    )
                )
            }
        }.onFailure { throw it }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getAccessToken(): Result<String> = runCatching {
        suspendCancellableCoroutine { cont ->
            val authService = mAuthService
                ?: return@suspendCancellableCoroutine cont.resumeWithException(AuthError.Local.AuthServiceNotInitialized())

            val authState = mAuthStateManager.current

            Timber.i("AuthState during start of getAccessToken: $authState")
            authState.performActionWithFreshTokens(authService) { accessToken, _, ex ->
                _isAuthorized.value = authState.isAuthorized
                when {
                    ex != null -> {
                        Timber.i("Exception: $ex")
                        cont.resumeWithException(ex)
                    }
                    accessToken != null -> {
                        Timber.i("Success: $accessToken")
                        cont.resume(accessToken) { CancellationException() }
                    }
                    else -> {
                        Timber.i("Other: no access token")
                        cont.resumeWithException(AuthError.Network.NoAccessToken())
                    }
                }
            }
        }
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

    private suspend fun saveUserInfo(userInfo: UserInfo) {
        withContext(Dispatchers.IO) {
            context.userInfoDataStore.updateData { userInfo }
            Timber.i("UserInfo saved to DataStore")
        }
    }

    private suspend fun loadUserInfoFromDataStore(): UserInfo? {
        Timber.i("Loading UserInfo from to DataStore")
        return withContext(Dispatchers.IO) {
            context.userInfoDataStore.data.map { it }.firstOrNull()
        }
    }

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

    private suspend fun fetchUserInfo(): JSONObject? {
        try {

            val accessToken = getAccessToken().fold({ it }, {}) as String?
            if (accessToken == null) return null

            val discovery: AuthorizationServiceDiscovery? =
                mAuthStateManager.current.getAuthorizationServiceConfiguration()?.discoveryDoc

            val userInfoEndpoint = mConfiguration.userInfoEndpointUri
                ?: discovery?.let { Uri.parse(it.userinfoEndpoint.toString()) }
                ?: throw AuthError.Local.MissingDiscoveryUriOrExplicitEndpoints()

            Timber.d("Endpoint: %s", AuthorizationServiceConfiguration.WELL_KNOWN_PATH)
            Timber.d("User info endpoint: $userInfoEndpoint")
            Timber.d("Access token: $accessToken")

            return withContext(Dispatchers.IO) {
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
            }
        } catch (e: AuthError.Network.NoAccessToken) {
            _isAuthorized.value = false
            return null
        } catch (e: java.util.concurrent.CancellationException) {
            return null
        } catch (ioEx: IOException) {
            Timber.e("Network error when querying userinfo endpoint $ioEx")
            return null
        } catch (jsonEx: JSONException) {
            Timber.e("Failed to parse userinfo response")
            return null
        } catch (e: Exception) {
            Timber.e("Other exception: $e")
            return null
        }
    }
}