package com.thesis.lumine.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thesis.lumine.data.model.AuthResponse
import com.thesis.lumine.data.repository.JewelryRepository
import com.thesis.lumine.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = JewelryRepository()
    private val sessionManager = SessionManager(application.applicationContext)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    // OTP flow — i-track kung naisend na ba yung code
    private val _otpSent = MutableStateFlow(false)
    val otpSent: StateFlow<Boolean> = _otpSent

    // i-hold muna yung email at password habang hindi pa tapos yung OTP step
    private var _pendingEmail    = ""
    private var _pendingPassword = ""

    init {
        // i-check agad kung may existing session bago pa man mag-load ng login screen
        checkExistingSession()
    }

    // i-restore yung session galing sa encrypted prefs para hindi na mag-login ulit
    private fun checkExistingSession() {
        if (sessionManager.isLoggedIn()) {
            val email        = sessionManager.getUserEmail()    ?: return
            val userId       = sessionManager.getUserId()       ?: return
            val accessToken  = sessionManager.getAccessToken()  ?: return
            val refreshToken = sessionManager.getRefreshToken() ?: ""
            val isAdmin      = sessionManager.isAdmin()

            _authState.value = AuthState.Success(
                AuthResponse(
                    accessToken  = accessToken,
                    refreshToken = refreshToken,
                    email        = email,
                    userId       = userId,
                    isAdmin      = isAdmin
                )
            )
        }
    }

    // email + password auth — i-validate muna bago i-send sa backend

    // i-register ang user — i-check email at password format bago mag-proceed
    fun register(email: String, password: String) {
        if (!validateEmail(email))       { _authState.value = AuthState.Error("Please use a valid email provider (e.g. Gmail, Outlook, Yahoo, iCloud)."); return }
        if (!validatePassword(password)) { _authState.value = AuthState.Error("Password must be at least 8 characters."); return }

        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val response = repository.register(email.trim(), password)

                when {
                    response.code() == 409 ->
                        _authState.value = AuthState.Error("An account with this email already exists.")
                    response.isSuccessful && response.body() != null ->
                        persistAndSucceed(response.body()!!)
                    else -> {
                        // hanapin yung tamang error message sa response body
                        val errorBody = response.errorBody()?.string() ?: ""
                        val detail = extractErrorMessage(errorBody)
                        _authState.value = AuthState.Error(detail)
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Network error. Check your connection.")
            }
        }
    }

    // i-parse yung JSON error galing sa backend para makuha yung tamang message
    private fun extractErrorMessage(errorBody: String): String {
        return try {
            // i-parse yung JSON error galing sa backend
            val json = org.json.JSONObject(errorBody)
            json.optString("details").ifBlank {
                json.optString("error").ifBlank { "Registration failed. Please try again." }
            }
        } catch (e: Exception) {
            "Registration failed. Please try again."
        }
    }

    // i-login ang user — 401 means maling credentials, handle mo yun
    fun login(email: String, password: String) {
        if (!validateEmail(email))    { _authState.value = AuthState.Error("Please use a valid email provider (e.g. Gmail, Outlook, Yahoo, iCloud)."); return }
        if (password.isBlank())       { _authState.value = AuthState.Error("Password is required."); return }

        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val response = repository.login(email.trim(), password)

                when {
                    response.code() == 401 ->
                        _authState.value = AuthState.Error("Incorrect email or password.")
                    response.isSuccessful && response.body() != null -> {
                        val auth = response.body()!!
                        persistAndSucceed(auth)
                    }
                    else ->
                        _authState.value = AuthState.Error("Login failed. Please try again.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Network error. Check your connection.")
            }
        }
    }

    // i-save yung session locally tapos i-update yung auth state sa Success
    private fun persistAndSucceed(auth: AuthResponse) {
        sessionManager.saveSession(
            accessToken  = auth.accessToken,
            refreshToken = auth.refreshToken,
            email        = auth.email,
            userId       = auth.userId,
            isAdmin      = auth.isAdmin
        )
        _authState.value = AuthState.Success(auth)
    }

    // OTP registration flow — dalawang steps: send OTP tapos verify

    // i-store muna yung credentials tapos mag-send ng OTP
    fun initRegisterOtp(email: String, password: String) {
        if (!validateEmail(email))       { _authState.value = AuthState.Error("Please use a valid email provider (e.g. Gmail, Outlook, Yahoo, iCloud)."); return }
        if (!validatePassword(password)) { _authState.value = AuthState.Error("Password must be at least 8 characters."); return }
        _pendingEmail    = email.trim()
        _pendingPassword = password
        sendOtp(email.trim())
    }

    // i-verify yung OTP code tapos i-create na yung account
    fun verifyAndRegister(token: String) {
        val email    = _pendingEmail
        val password = _pendingPassword
        if (email.isBlank()) { _authState.value = AuthState.Error("Session expired. Please try again."); return }
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val response = repository.verifyAndRegister(email, token, password)
                when {
                    response.isSuccessful && response.body() != null ->
                        persistAndSucceed(response.body()!!)
                    else ->
                        _authState.value = AuthState.Error("Invalid or expired OTP code.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Network error")
            }
        }
    }

    // i-expose yung pending email para makita ng OTP screen
    fun getPendingEmail(): String = _pendingEmail

    // OTP auth (existing flow para sa magic link login)

    // i-send yung OTP sa email — handle yung 409 (existing email), 429 (rate limit)
    fun sendOtp(email: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val response = repository.sendOtp(email)
                when {
                    response.code() == 409 ->
                        _authState.value = AuthState.Error("An account with this email already exists. Please log in instead.")
                    response.code() == 429 ->
                        _authState.value = AuthState.Error("Too many requests. Please wait 60 seconds.")
                    response.isSuccessful -> {
                        _otpSent.value = true
                        _authState.value = AuthState.OtpSent
                    }
                    else -> _authState.value = AuthState.Error("Failed to send OTP. Check your email address.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Network error")
            }
        }
    }

    // i-verify yung OTP token — pag tama, i-persist yung session
    fun verifyOtp(email: String, token: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val response = repository.verifyOtp(email, token)
                if (response.isSuccessful && response.body() != null) {
                    persistAndSucceed(response.body()!!)
                } else {
                    _authState.value = AuthState.Error("Invalid OTP code")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Network error")
            }
        }
    }

    // i-reset yung OTP sent flag para makapag-resend ulit
    fun resetOtpSent() {
        _otpSent.value = false
    }

    // session management — i-clear lahat pag nag-logout

    // i-logout yung user — burahin yung session at i-reset lahat ng state
    fun logout() {
        sessionManager.clearSession()
        _authState.value = AuthState.Idle
        _otpSent.value = false
    }

    // validation helpers — simple checks para sa email at password format

    // short whitelist of accepted email domains
    private val allowedDomains = setOf(
        "gmail.com", "googlemail.com",
        "yahoo.com", "yahoo.co.uk", "yahoo.com.ph",
        "outlook.com", "hotmail.com", "live.com", "live.com.ph",
        "icloud.com", "me.com", "mac.com",
        "protonmail.com", "proton.me",
        "aol.com", "zoho.com", "yandex.com",
        "gmx.com", "gmx.net", "mail.com"
    )

    // parse the email as a URI to extract the host (domain), then check the whitelist
    // "//${email}" makes the URI treat "user@gmail.com" as authority → uri.host = "gmail.com"
    private fun validateEmail(email: String): Boolean {
        return try {
            val uri = java.net.URI("//${email.trim()}")
            val host = uri.host?.lowercase() ?: return false
            host in allowedDomains || host.endsWith(".edu") || host.endsWith(".edu.ph")
        } catch (e: Exception) {
            false
        }
    }

    // minimum 8 characters lang ang requirement para sa password
    private fun validatePassword(password: String): Boolean =
        password.length >= 8
}

sealed class AuthState {
    object Idle    : AuthState()
    object Loading : AuthState()
    object OtpSent : AuthState()
    data class Success(val authResponse: AuthResponse) : AuthState()
    data class Error(val message: String) : AuthState()
}
