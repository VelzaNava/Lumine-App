package com.thesis.lumine.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores the signed-in session in encrypted preferences (AES-256), so tokens
 * are never readable from the device's storage in plain text.
 */
class SessionManager(context: Context) {

    private val preferences: SharedPreferences = openPreferences(context.applicationContext)

    fun saveSession(
        accessToken: String,
        refreshToken: String,
        email: String,
        userId: String,
        isAdmin: Boolean = false
    ) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_ID, userId)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putBoolean(KEY_IS_ADMIN, isAdmin)
            .apply()
    }

    /** Stores a refreshed token pair; the rest of the session is unchanged. */
    fun updateTokens(accessToken: String, refreshToken: String) {
        preferences.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun getAccessToken(): String? = preferences.getString(KEY_ACCESS_TOKEN, null)

    fun getRefreshToken(): String? = preferences.getString(KEY_REFRESH_TOKEN, null)

    fun getUserEmail(): String? = preferences.getString(KEY_USER_EMAIL, null)

    fun getUserId(): String? = preferences.getString(KEY_USER_ID, null)

    fun isLoggedIn(): Boolean = preferences.getBoolean(KEY_IS_LOGGED_IN, false)

    fun isAdmin(): Boolean = preferences.getBoolean(KEY_IS_ADMIN, false)

    fun clearSession() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val TAG = "SessionManager"
        const val PREFS_NAME = "lumine_secure_prefs"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_USER_ID = "user_id"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val KEY_IS_ADMIN = "is_admin"

        /**
         * The encryption key exists only in this phone's keystore, so a copy of the
         * file restored from a backup (reinstall, new phone) can't be decrypted and
         * opening it throws. Start signed out instead of crashing on launch.
         */
        fun openPreferences(context: Context): SharedPreferences = try {
            create(context)
        } catch (e: Exception) {
            Log.w(TAG, "Saved session can't be read; starting signed out", e)
            context.deleteSharedPreferences(PREFS_NAME)
            create(context)
        }

        fun create(context: Context): SharedPreferences = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
