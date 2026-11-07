package com.thesis.lumine.data.api

import com.thesis.lumine.utils.SessionManager
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/** Adds `Authorization: Bearer <access token>` to every call except the auth endpoints. */
class AuthInterceptor(private val session: SessionManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = session.getAccessToken()

        if (token.isNullOrBlank() || request.isAuthEndpoint()) {
            return chain.proceed(request)
        }

        return chain.proceed(request.withAccessToken(token))
    }
}

/** Login, sign-up and password-reset calls: they create sessions rather than use one. */
internal fun Request.isAuthEndpoint(): Boolean = url.encodedPath.contains("/api/auth/")

internal fun Request.withAccessToken(token: String): Request =
    newBuilder().header("Authorization", "Bearer $token").build()
