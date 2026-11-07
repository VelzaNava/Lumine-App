package com.thesis.lumine.data.model

import com.google.gson.annotations.SerializedName

data class LoginRequest(val email: String, val password: String)

data class OtpRequest(val email: String)

data class VerifyAndRegisterRequest(val email: String, val token: String, val password: String)

data class ForgotPasswordRequest(val email: String)

data class ResetPasswordRequest(val email: String, val token: String, val newPassword: String)

data class RefreshSessionRequest(val refreshToken: String)

data class DeleteAccountRequest(val password: String)

// Supabase Auth, for setting an admin's password (see SupabaseAuthService).

data class SupabaseCodeRequest(
    val email: String,
    @SerializedName("create_user") val createUser: Boolean = false
)

/** "magiclink" is Supabase's verification type for codes sent by its otp endpoint. */
data class SupabaseVerifyRequest(val email: String, val token: String, val type: String = "magiclink")

data class SupabasePasswordRequest(val password: String)

data class SupabaseSession(@SerializedName("access_token") val accessToken: String?)

/**
 * A signed-in session. [accessToken] authorizes API calls for about an hour;
 * [refreshToken] (single-use) is traded for a new pair when it expires.
 */
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val email: String,
    val userId: String,
    val isAdmin: Boolean = false
)
