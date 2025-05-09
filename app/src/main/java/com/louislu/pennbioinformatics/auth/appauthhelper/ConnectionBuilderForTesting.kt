/*
 * Copyright 2016 The AppAuth for Android Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file is a Kotlin conversion of the original Java version from AppAuth for Android.
 * Converted and modified by Louis Lu on 3/14/2025.
 */
package com.louislu.logintester.appauthhelper

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import net.openid.appauth.Preconditions
import net.openid.appauth.connectivity.ConnectionBuilder
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * An example implementation of [ConnectionBuilder] that permits connecting to http
 * links, and ignores certificates for https connections. *THIS SHOULD NOT BE USED IN PRODUCTION
 * CODE*. It is intended to facilitate easier testing of AppAuth against development servers
 * only.
 */
class ConnectionBuilderForTesting private constructor() : ConnectionBuilder {
    @Throws(IOException::class)
    override fun openConnection(uri: Uri): HttpURLConnection {
        Preconditions.checkNotNull(uri, "url must not be null")
        Preconditions.checkArgument(
            HTTP == uri.scheme || HTTPS == uri.scheme,
            "scheme or uri must be http or https"
        )
        val conn = URL(uri.toString()).openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECTION_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS
        conn.instanceFollowRedirects = false

        if (conn is HttpsURLConnection && TRUSTING_CONTEXT != null) {
            val httpsConn = conn
            httpsConn.sslSocketFactory = TRUSTING_CONTEXT.socketFactory
            httpsConn.hostnameVerifier = ANY_HOSTNAME_VERIFIER
        }

        return conn
    }

    companion object {
        val INSTANCE: ConnectionBuilderForTesting = ConnectionBuilderForTesting()

        private const val TAG = "ConnBuilder"

        private val CONNECTION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(15).toInt()
        private val READ_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10).toInt()

        private const val HTTP = "http"
        private const val HTTPS = "https"

        @SuppressLint("TrustAllX509TrustManager", "CustomX509TrustManager")
        private val ANY_CERT_MANAGER = arrayOf<TrustManager>(object : X509TrustManager {
            override fun getAcceptedIssuers(): Array<X509Certificate>? {
                return null
            }

            override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}

            override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
        }
        )

        @SuppressLint("BadHostnameVerifier")
        private val ANY_HOSTNAME_VERIFIER = HostnameVerifier { hostname, session -> true }

        private val TRUSTING_CONTEXT: SSLContext?

        init {
            var context: SSLContext?
            try {
                context = SSLContext.getInstance("SSL")
            } catch (e: NoSuchAlgorithmException) {
                Log.e("ConnBuilder", "Unable to acquire SSL context")
                context = null
            }

            var initializedContext: SSLContext? = null
            if (context != null) {
                try {
                    context.init(null, ANY_CERT_MANAGER, SecureRandom())
                    initializedContext = context
                } catch (e: KeyManagementException) {
                    Log.e(TAG, "Failed to initialize trusting SSL context")
                }
            }

            TRUSTING_CONTEXT = initializedContext
        }
    }
}
