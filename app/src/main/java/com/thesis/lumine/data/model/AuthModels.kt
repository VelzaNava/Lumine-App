package com.thesis.lumine.data.model

data class OtpRequest(val email: String)

data class VerifyOtpRequest(val email: String, val token: String)

data class RegisterRequest(val email: String, val password: String)
data class VerifyAndRegisterRequest(val email: String, val token: String, val password: String)

data class LoginRequest(val email: String, val password: String)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val email: String,
    val userId: String,
    val isAdmin: Boolean = false
)