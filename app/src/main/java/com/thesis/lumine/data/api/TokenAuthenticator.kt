package com.thesis.lumine.data.api

import com.thesis.lumine.data.model.AuthResponse
import com.thesis.lumine.data.model.RefreshSessionRequest
import com.thesis.lumine.utils.SessionManager
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import java.io.IOException

/** The one endpoint called outside the authenticated client. */
interface AuthRefreshApi {
    @POST("api/auth/refresh")
    fun refresh(@Body request: RefreshSessionRequest): Call<AuthResponse>
}

/**
 * Handles an expired access token (HTTP 401): trades the refresh token for a
 * new session once, then retries the original request with it.
 *
 * Supabase refresh tokens are single-use, so concurrent 401s are serialized:
 * the first request refreshes, the others simply retry with the new token.
 * If the refresh token itself is rejected the session is over - it is cleared
 * and [SessionEvents] tells the UI to return to the login screen.
 */
class TokenAuthenticator(
    private val session: SessionManager,
    private val refreshApi: AuthRefreshApi
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val request = response.request

        // A 401 from login or sign-up means wrong credentials, not an expired session.
        if (request.isAuthEndpoint()) return null

        // Already retried with a fresh token and still refused: give up.
        if (response.priorResponse != null) {
            endSession()
            return null
        }

        synchronized(lock) {
            val sentToken = request.header("Authorization")?.removePrefix("Bearer ")
            val currentToken = session.getAccessToken()

            // Another request refreshed while this one waited for the lock.
            if (!currentToken.isNullOrBlank() && currentToken != sentToken) {
                return request.withAccessToken(currentToken)
            }

            val refreshToken = session.getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                endSession()
                return null
            }

            val refreshed = try {
                refreshApi.refresh(RefreshSessionRequest(refreshToken)).execute()
            } catch (e: IOException) {
                // Offline: keep the session and let the caller show a network error.
                return null
            }

            val newSession = refreshed.body()
            if (!refreshed.isSuccessful || newSession == null) {
                // 400/401: the refresh token is invalid or used up. Anything else
                // (server hiccup, rate limit) is temporary, so keep the session.
                if (refreshed.code() == 400 || refreshed.code() == 401) endSession()
                return null
            }

            session.updateTokens(newSession.accessToken, newSession.refreshToken)
            return request.withAccessToken(newSession.accessToken)
        }
    }

    private fun endSession() {
        session.clearSession()
        SessionEvents.notifyExpired()
    }
}
