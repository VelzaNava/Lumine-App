package com.thesis.lumine.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thesis.lumine.data.api.errorMessage
import com.thesis.lumine.data.api.retryAfterSeconds
import com.thesis.lumine.data.api.toUserMessage
import com.thesis.lumine.data.model.AuthResponse
import com.thesis.lumine.data.repository.JewelryRepository
import com.thesis.lumine.utils.SessionManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.URI
import java.net.URISyntaxException

/** Login, sign-up (emailed code) and password reset. */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = JewelryRepository()
    private val sessionManager = SessionManager(application.applicationContext)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    /** Seconds left before another sign-in attempt is allowed; 0 means go ahead. */
    private val _loginWaitSeconds = MutableStateFlow(0)
    val loginWaitSeconds: StateFlow<Int> = _loginWaitSeconds
    private var loginWait: Job? = null

    private val _passwordChange = MutableStateFlow<PasswordChangeState>(PasswordChangeState.Idle)
    val passwordChange: StateFlow<PasswordChangeState> = _passwordChange

    // Held between sign-up step 1 (send code) and step 2 (verify code).
    private var pendingEmail = ""
    private var pendingPassword = ""

    // Held between password-reset step 1 and step 2.
    private var pendingResetEmail = ""
    private var resetThroughSupabase = false

    init {
        restoreSession()
    }

    /** Resumes a saved session so a returning user skips the login screen. */
    private fun restoreSession() {
        if (!sessionManager.isLoggedIn()) return

        val email = sessionManager.getUserEmail() ?: return
        val userId = sessionManager.getUserId() ?: return
        val accessToken = sessionManager.getAccessToken() ?: return

        _authState.value = AuthState.Success(
            AuthResponse(
                accessToken = accessToken,
                refreshToken = sessionManager.getRefreshToken().orEmpty(),
                email = email,
                userId = userId,
                isAdmin = sessionManager.isAdmin()
            )
        )
    }

    // Login

    fun login(email: String, password: String) {
        if (_loginWaitSeconds.value > 0) return
        if (!isAllowedEmail(email)) return fail(INVALID_EMAIL)
        if (password.isBlank()) return fail("Password is required.")

        launchAuthCall {
            val response = repository.login(email.trim(), password)
            val session = response.body()
            when {
                response.isSuccessful && session != null -> persistAndSucceed(session)
                response.code() == 401 -> {
                    startLoginWait(response.retryAfterSeconds(WRONG_PASSWORD_WAIT_SECONDS))
                    fail("Wrong email or password.")
                }
                response.code() == 429 -> {
                    startLoginWait(response.retryAfterSeconds(WRONG_PASSWORD_WAIT_SECONDS))
                    fail(response.errorMessage(TOO_MANY_REQUESTS))
                }
                else -> fail(response.errorMessage("Login failed. Please try again."))
            }
        }
    }

    /** The server makes an email wait after a wrong password; count it down on the button. */
    private fun startLoginWait(seconds: Int) {
        loginWait?.cancel()
        loginWait = viewModelScope.launch {
            for (left in seconds downTo 1) {
                _loginWaitSeconds.value = left
                delay(1_000)
            }
            _loginWaitSeconds.value = 0
        }
    }

    // Sign-up

    /** Step 1: remembers the chosen credentials and emails a verification code. */
    fun initRegisterOtp(email: String, password: String) {
        if (!isAllowedEmail(email)) return fail(INVALID_EMAIL)
        if (!isValidPassword(password)) return fail(SHORT_PASSWORD)

        pendingEmail = email.trim()
        pendingPassword = password

        launchAuthCall {
            val response = repository.sendOtp(pendingEmail)
            when {
                response.isSuccessful -> _authState.value = AuthState.OtpSent
                response.code() == 409 ->
                    fail("An account with this email already exists. Please log in instead.")
                response.code() == 429 -> fail(TOO_MANY_REQUESTS)
                else -> fail(response.errorMessage("Couldn't send the code. Please check your email address."))
            }
        }
    }

    /** Step 2: verifies the code, creates the account and signs in. */
    fun verifyAndRegister(code: String) {
        if (pendingEmail.isBlank()) return fail(SESSION_LOST)

        launchAuthCall {
            val response = repository.verifyAndRegister(pendingEmail, code, pendingPassword)
            val session = response.body()
            when {
                response.isSuccessful && session != null -> persistAndSucceed(session)
                response.code() == 401 -> fail(INVALID_CODE)
                else -> fail(response.errorMessage(INVALID_CODE))
            }
        }
    }

    fun getPendingEmail(): String = pendingEmail

    /** Emails a fresh sign-up code to the pending address; the previous code stops working. */
    fun resendSignUpCode() {
        if (pendingEmail.isBlank()) return fail(SESSION_LOST)

        launchAuthCall {
            val response = repository.sendOtp(pendingEmail)
            when {
                response.isSuccessful -> _authState.value = AuthState.OtpSent
                response.code() == 429 -> fail(TOO_MANY_REQUESTS)
                else -> fail(response.errorMessage("Couldn't send a new code. Please try again."))
            }
        }
    }

    // Password reset. The server refuses admin accounts (403), so theirs goes to Supabase directly.

    /** Step 1: emails a reset code to an existing account. */
    fun forgotPassword(email: String) {
        if (!isAllowedEmail(email)) return fail(INVALID_EMAIL)

        val trimmed = email.trim()
        launchAuthCall {
            val response = repository.forgotPassword(trimmed)
            when {
                response.isSuccessful -> {
                    pendingResetEmail = trimmed
                    resetThroughSupabase = false
                    _authState.value = AuthState.OtpSent
                }
                response.code() == 403 -> sendAdminResetCode(trimmed)
                response.code() == 404 -> fail("No account found with this email.")
                response.code() == 429 -> fail(TOO_MANY_REQUESTS)
                else -> fail(response.errorMessage("Couldn't send the reset code. Please try again."))
            }
        }
    }

    /** Step 2: verifies the code, sets the new password and signs in. */
    fun resetPassword(code: String, newPassword: String) {
        if (pendingResetEmail.isBlank()) return fail(SESSION_LOST)
        if (!isValidPassword(newPassword)) return fail(SHORT_PASSWORD)

        launchAuthCall {
            if (resetThroughSupabase) return@launchAuthCall finishAdminReset(code, newPassword)

            val response = repository.resetPassword(pendingResetEmail, code, newPassword)
            val session = response.body()
            when {
                response.isSuccessful && session != null -> persistAndSucceed(session)
                response.code() == 401 -> fail(INVALID_CODE)
                response.code() == 403 ->
                    fail("This account can't use self-service password reset. Please contact support.")
                // 400: e.g. the new password is the same as the old one.
                else -> fail(response.errorMessage(INVALID_CODE))
            }
        }
    }

    fun getPendingResetEmail(): String = pendingResetEmail

    /** Emails a fresh reset code to the pending address. */
    fun resendResetCode() {
        if (pendingResetEmail.isBlank()) return fail(SESSION_LOST)

        launchAuthCall {
            if (resetThroughSupabase) return@launchAuthCall sendAdminResetCode(pendingResetEmail)

            val response = repository.forgotPassword(pendingResetEmail)
            when {
                response.isSuccessful -> _authState.value = AuthState.OtpSent
                response.code() == 429 -> fail(TOO_MANY_REQUESTS)
                else -> fail(response.errorMessage("Couldn't send a new code. Please try again."))
            }
        }
    }

    private suspend fun sendAdminResetCode(email: String) {
        if (!repository.canSetAdminPassword) return fail(ADMIN_PASSWORD_UNAVAILABLE)

        val response = repository.sendAdminResetCode(email)
        when {
            response.isSuccessful -> {
                pendingResetEmail = email
                resetThroughSupabase = true
                _authState.value = AuthState.OtpSent
            }
            response.code() == 429 -> fail(TOO_MANY_REQUESTS)
            else -> fail("Couldn't send the reset code. Please try again.")
        }
    }

    /** Verifies the code with Supabase, sets the new password, then signs in through the server. */
    private suspend fun finishAdminReset(code: String, newPassword: String) {
        val verified = repository.verifyAdminResetCode(pendingResetEmail, code)
        val accessToken = verified.body()?.accessToken
        if (!verified.isSuccessful || accessToken == null) {
            return fail(if (verified.code() == 429) TOO_MANY_REQUESTS else INVALID_CODE)
        }

        val updated = repository.setAdminPassword(accessToken, newPassword)
        if (!updated.isSuccessful) {
            // The code is used up by now, so another try needs a new one.
            return fail(
                if (updated.code() == 422) "Please choose a different password, then request a new code."
                else "Your password couldn't be saved. Please request a new code and try again."
            )
        }

        // Signing in through the server is what marks the session as an admin's.
        val login = repository.login(pendingResetEmail, newPassword)
        val session = login.body()
        if (login.isSuccessful && session != null) {
            persistAndSucceed(session)
        } else {
            fail("Your password was changed. Please log in with the new one.")
        }
    }

    // Change password (admin dashboard)

    fun getSignedInEmail(): String = sessionManager.getUserEmail().orEmpty()

    /**
     * Checks [currentPassword] by signing in with it, then sets the new one. Kept out of
     * [authState] because the admin stays signed in, and the email never changes: it's
     * what makes the account an admin.
     */
    fun changeAdminPassword(currentPassword: String, newPassword: String) {
        if (!repository.canSetAdminPassword) {
            _passwordChange.value = PasswordChangeState.Error(ADMIN_PASSWORD_UNAVAILABLE)
            return
        }

        viewModelScope.launch {
            _passwordChange.value = PasswordChangeState.Saving
            _passwordChange.value = try {
                savePasswordChange(getSignedInEmail(), currentPassword, newPassword)
            } catch (e: Exception) {
                PasswordChangeState.Error(e.toUserMessage())
            }
        }
    }

    private suspend fun savePasswordChange(
        email: String, currentPassword: String, newPassword: String
    ): PasswordChangeState {
        val login = repository.login(email, currentPassword)
        val session = login.body()
        if (!login.isSuccessful || session == null) {
            return PasswordChangeState.Error(
                when (login.code()) {
                    401 -> {
                        val wait = login.retryAfterSeconds(WRONG_PASSWORD_WAIT_SECONDS)
                        "That isn't your current password. You can try again in $wait seconds."
                    }
                    429 -> login.errorMessage(TOO_MANY_REQUESTS)
                    else -> login.errorMessage("Couldn't check your password. Please try again.")
                }
            )
        }

        // Saved first: changing the password ends every session except this new one.
        persistAndSucceed(session)

        val updated = repository.setAdminPassword(session.accessToken, newPassword)
        return when {
            updated.isSuccessful -> PasswordChangeState.Done
            updated.code() == 422 -> PasswordChangeState.Error("Please choose a different password.")
            else -> PasswordChangeState.Error("Your password couldn't be saved. Please try again.")
        }
    }

    /** Clears the dialog's state when it opens or closes. */
    fun resetPasswordChange() {
        _passwordChange.value = PasswordChangeState.Idle
    }

    // Screen state

    /**
     * Called when an auth screen opens. The auth screens share this ViewModel,
     * so without it a failed login's error - or a "code sent" from sign-up -
     * would show up on (or even navigate) the next screen.
     */
    fun resetTransientState() {
        val state = _authState.value
        if (state is AuthState.Error || state is AuthState.OtpSent) _authState.value = AuthState.Idle
    }

    /** Clears a server error once the user starts correcting their input. */
    fun clearError() {
        if (_authState.value is AuthState.Error) _authState.value = AuthState.Idle
    }

    // Session

    fun logout() {
        sessionManager.clearSession()
        _authState.value = AuthState.Idle
    }

    private fun persistAndSucceed(session: AuthResponse) {
        sessionManager.saveSession(
            accessToken = session.accessToken,
            refreshToken = session.refreshToken,
            email = session.email,
            userId = session.userId,
            isAdmin = session.isAdmin
        )
        _authState.value = AuthState.Success(session)
    }

    /** Shows the loading state, runs [block], and turns a thrown exception into a readable error. */
    private fun launchAuthCall(block: suspend () -> Unit) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                block()
            } catch (e: Exception) {
                fail(e.toUserMessage())
            }
        }
    }

    private fun fail(message: String) {
        _authState.value = AuthState.Error(message)
    }

    // Validation

    /** Accepts well-known email providers and school (.edu / .edu.ph) addresses. */
    private fun isAllowedEmail(email: String): Boolean {
        // Parsed as a URI authority ("//user@host"), which rejects spaces and other stray characters.
        val uri = try {
            URI("//${email.trim()}")
        } catch (e: URISyntaxException) {
            return false
        }

        val domain = uri.host?.lowercase() ?: return false
        if (uri.userInfo.isNullOrBlank()) return false
        return domain in ALLOWED_DOMAINS || domain.endsWith(".edu") || domain.endsWith(".edu.ph")
    }

    private fun isValidPassword(password: String): Boolean = password.length >= MIN_PASSWORD_LENGTH

    private companion object {
        const val MIN_PASSWORD_LENGTH = 8
        const val WRONG_PASSWORD_WAIT_SECONDS = 10

        const val INVALID_EMAIL = "Please use a valid email provider (e.g. Gmail, Outlook, Yahoo, iCloud)."
        const val SHORT_PASSWORD = "Password must be at least 8 characters."
        const val INVALID_CODE = "Invalid or expired code."
        const val SESSION_LOST = "Your session expired. Please start again."
        const val TOO_MANY_REQUESTS = "Too many attempts. Please wait a minute and try again."
        const val ADMIN_PASSWORD_UNAVAILABLE = "Changing an admin password isn't set up in this version of the app."

        val ALLOWED_DOMAINS = setOf(
            "gmail.com", "googlemail.com",
            "yahoo.com", "yahoo.co.uk", "yahoo.com.ph",
            "outlook.com", "hotmail.com", "live.com", "live.com.ph",
            "icloud.com", "me.com", "mac.com",
            "protonmail.com", "proton.me",
            "aol.com", "zoho.com", "yandex.com",
            "gmx.com", "gmx.net", "mail.com"
        )
    }
}

sealed class PasswordChangeState {
    data object Idle : PasswordChangeState()
    data object Saving : PasswordChangeState()
    data object Done : PasswordChangeState()
    data class Error(val message: String) : PasswordChangeState()
}

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data object OtpSent : AuthState()
    data class Success(val authResponse: AuthResponse) : AuthState()
    data class Error(val message: String) : AuthState()
}
