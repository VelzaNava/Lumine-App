package com.thesis.lumine.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thesis.lumine.data.api.errorMessage
import com.thesis.lumine.data.api.toUserMessage
import com.thesis.lumine.data.model.AdminUserInfo
import com.thesis.lumine.data.model.UpdateProfileRequest
import com.thesis.lumine.data.model.UserProfileResponse
import com.thesis.lumine.data.repository.JewelryRepository
import com.thesis.lumine.utils.ImageCompressor
import com.thesis.lumine.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The signed-in user's profile and favorites, plus the admin panel's user
 * management. Each operation has its own progress flag, so saving a profile
 * or uploading a photo never blanks a screen that already shows data.
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = JewelryRepository()
    private val sessionManager = SessionManager(application.applicationContext)

    // Own profile

    private val _profile = MutableStateFlow(UserProfileResponse())
    val profile: StateFlow<UserProfileResponse> = _profile

    /** True only while the profile loads for the first time (nothing to show yet). */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    /** True while profile edits are being saved. */
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _isUploadingAvatar = MutableStateFlow(false)
    val isUploadingAvatar: StateFlow<Boolean> = _isUploadingAvatar

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private var profileLoaded = false

    // Own favorites

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds

    /** Becomes true after the first favorites load, so screens can tell "loading" from "none". */
    private val _favoritesLoaded = MutableStateFlow(false)
    val favoritesLoaded: StateFlow<Boolean> = _favoritesLoaded

    // Admin: user management

    private val _adminUsers = MutableStateFlow<List<AdminUserInfo>>(emptyList())
    val adminUsers: StateFlow<List<AdminUserInfo>> = _adminUsers

    /** Spinner only on the first load; revisiting the tab refreshes silently. */
    private val _isLoadingAdminUsers = MutableStateFlow(false)
    val isLoadingAdminUsers: StateFlow<Boolean> = _isLoadingAdminUsers

    private val _selectedUser = MutableStateFlow<AdminUserInfo?>(null)
    val selectedUser: StateFlow<AdminUserInfo?> = _selectedUser

    private val _userFavoriteIds = MutableStateFlow<List<String>>(emptyList())
    val userFavoriteIds: StateFlow<List<String>> = _userFavoriteIds

    private val _isLoadingUserFavorites = MutableStateFlow(false)
    val isLoadingUserFavorites: StateFlow<Boolean> = _isLoadingUserFavorites

    private val _deleteSuccess = MutableStateFlow(false)
    val deleteSuccess: StateFlow<Boolean> = _deleteSuccess

    // Profile

    fun loadProfile() {
        val userId = sessionManager.getUserId() ?: return
        viewModelScope.launch {
            val firstLoad = !profileLoaded
            if (firstLoad) _isLoading.value = true
            try {
                val response = repository.getProfile(userId)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    _profile.value = body
                    profileLoaded = true
                } else {
                    _error.value = response.errorMessage("Couldn't load your profile.")
                }
            } catch (e: Exception) {
                _error.value = e.toUserMessage()
            } finally {
                if (firstLoad) _isLoading.value = false
            }
        }
    }

    /** Saves the editable fields; a blank mobile number is stored as "none". */
    fun updateProfile(username: String, firstName: String, lastName: String, mobileNumber: String?) {
        val userId = sessionManager.getUserId() ?: return
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val request = UpdateProfileRequest(
                    username = username.trim(),
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    mobileNumber = mobileNumber?.trim()?.ifBlank { null }
                )
                val response = repository.updateProfile(userId, request)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    _profile.value = body
                    _saveSuccess.value = true
                } else {
                    _error.value = response.errorMessage("Couldn't save your profile. Please try again.")
                }
            } catch (e: Exception) {
                _error.value = e.toUserMessage()
            } finally {
                _isSaving.value = false
            }
        }
    }

    /** Compresses the picked photo off the main thread, then uploads it. */
    fun uploadAvatar(uri: Uri) {
        val userId = sessionManager.getUserId() ?: return
        viewModelScope.launch {
            _isUploadingAvatar.value = true
            try {
                val context = getApplication<Application>().applicationContext
                val part = withContext(Dispatchers.IO) { ImageCompressor.toUploadPart(context, uri, "avatar") }
                if (part == null) {
                    _error.value = "That photo couldn't be read. Please choose another one."
                    return@launch
                }

                val response = repository.uploadAvatar(userId, part)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    _profile.value = _profile.value.copy(avatarUrl = body.avatarUrl)
                } else {
                    _error.value = response.errorMessage("Couldn't upload your photo. Please try again.")
                }
            } catch (e: Exception) {
                _error.value = e.toUserMessage()
            } finally {
                _isUploadingAvatar.value = false
            }
        }
    }

    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }

    // Account deletion

    private val _isDeletingAccount = MutableStateFlow(false)
    val isDeletingAccount: StateFlow<Boolean> = _isDeletingAccount

    /** Shown inside the confirmation dialog, so a wrong password can be retyped in place. */
    private val _deleteAccountError = MutableStateFlow<String?>(null)
    val deleteAccountError: StateFlow<String?> = _deleteAccountError

    /** Permanently deletes the signed-in account; [onDeleted] runs once the server confirms. */
    fun deleteAccount(password: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            _isDeletingAccount.value = true
            _deleteAccountError.value = null
            try {
                val response = repository.deleteAccount(password)
                if (response.isSuccessful) {
                    onDeleted()
                } else {
                    _deleteAccountError.value =
                        response.errorMessage("Couldn't delete your account. Please try again.")
                }
            } catch (e: Exception) {
                _deleteAccountError.value = e.toUserMessage()
            } finally {
                _isDeletingAccount.value = false
            }
        }
    }

    fun clearDeleteAccountError() {
        _deleteAccountError.value = null
    }

    fun clearError() {
        _error.value = null
    }

    // Favorites

    fun loadFavorites() {
        val userId = sessionManager.getUserId() ?: return
        viewModelScope.launch {
            try {
                val response = repository.getFavorites(userId)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    _favoriteIds.value = body.toSet()
                }
            } catch (e: Exception) {
                _error.value = e.toUserMessage()
            } finally {
                _favoritesLoaded.value = true
            }
        }
    }

    /**
     * Flips the heart right away, then saves it. If the server call fails it flips back.
     */
    fun toggleFavorite(jewelryId: String) {
        val userId = sessionManager.getUserId() ?: return
        val wasFavorite = jewelryId in _favoriteIds.value

        _favoriteIds.value = if (wasFavorite) _favoriteIds.value - jewelryId else _favoriteIds.value + jewelryId

        viewModelScope.launch {
            val succeeded = try {
                val response = if (wasFavorite) {
                    repository.removeFavorite(userId, jewelryId)
                } else {
                    repository.addFavorite(userId, jewelryId)
                }
                response.isSuccessful
            } catch (e: Exception) {
                false
            }

            if (!succeeded) {
                _favoriteIds.value = if (wasFavorite) _favoriteIds.value + jewelryId else _favoriteIds.value - jewelryId
                _error.value = "Couldn't update your favorites. Please try again."
            }
        }
    }

    // Admin: user management

    /** Loads every account except the signed-in admin's own. */
    fun loadAdminUsers() {
        viewModelScope.launch {
            val firstLoad = _adminUsers.value.isEmpty()
            if (firstLoad) _isLoadingAdminUsers.value = true
            try {
                val response = repository.getAdminUsers()
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    // Leaving the admin out also keeps "Remove account" away from their own account.
                    val ownId = sessionManager.getUserId()
                    _adminUsers.value = body.filter { it.userId != ownId }
                } else {
                    _error.value = response.errorMessage("Couldn't load the user list.")
                }
            } catch (e: Exception) {
                _error.value = e.toUserMessage()
            } finally {
                if (firstLoad) _isLoadingAdminUsers.value = false
            }
        }
    }

    fun selectUser(user: AdminUserInfo) {
        _selectedUser.value = user
    }

    fun clearSelectedUser() {
        _selectedUser.value = null
    }

    /** Clears first, so the sheet never shows the previous user's favorites while loading. */
    fun loadUserFavorites(userId: String) {
        viewModelScope.launch {
            _userFavoriteIds.value = emptyList()
            _isLoadingUserFavorites.value = true
            try {
                val response = repository.getUserFavorites(userId)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    _userFavoriteIds.value = body
                }
            } catch (e: Exception) {
                _error.value = e.toUserMessage()
            } finally {
                _isLoadingUserFavorites.value = false
            }
        }
    }

    fun deleteUser(userId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = repository.deleteUser(userId)
                if (response.isSuccessful) {
                    _adminUsers.value = _adminUsers.value.filter { it.userId != userId }
                    _deleteSuccess.value = true
                    onDone()
                } else {
                    _error.value = response.errorMessage("Couldn't remove the account. Please try again.")
                }
            } catch (e: Exception) {
                _error.value = e.toUserMessage()
            }
        }
    }

    fun resetDeleteSuccess() {
        _deleteSuccess.value = false
    }
}
