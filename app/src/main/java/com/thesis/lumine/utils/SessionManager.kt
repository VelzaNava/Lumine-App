package com.thesis.lumine.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {

    // i-setup yung encrypted storage gamit AES256 — para hindi basta mabasa ang tokens
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // i-create yung encrypted shared preferences para sa secure na pag-store ng session
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "lumine_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_ACCESS_TOKEN  = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_EMAIL    = "user_email"
        private const val KEY_USER_ID       = "user_id"
        private const val KEY_IS_LOGGED_IN  = "is_logged_in"
        private const val KEY_IS_ADMIN      = "is_admin"
    }

    // i-save yung lahat ng session data pagkatapos mag-login o mag-register
    fun saveSession(
        accessToken: String,
        refreshToken: String,
        email: String,
        userId: String,
        isAdmin: Boolean = false
    ) {
        sharedPreferences.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_ID, userId)
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_IS_ADMIN, isAdmin)
            apply()
        }
    }

    // kunin yung access token para gamitin sa API calls
    fun getAccessToken(): String? {
        return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    // kunin yung refresh token pag mag-expire na yung access token
    fun getRefreshToken(): String? {
        return sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    // kunin yung email ng naka-login na user
    fun getUserEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }

    // kunin yung userId — kailangan para sa lahat ng profile at favorite API calls
    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    // i-check kung may active session pa — para hindi na mag-login ulit
    fun isLoggedIn(): Boolean = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)

    // i-check kung admin yung naka-login para ma-redirect sa tamang screen
    fun isAdmin(): Boolean = sharedPreferences.getBoolean(KEY_IS_ADMIN, false)

    // burahin lahat ng session data — ginagamit pag mag-logout
    fun clearSession() {
        sharedPreferences.edit().clear().apply()
    }
}