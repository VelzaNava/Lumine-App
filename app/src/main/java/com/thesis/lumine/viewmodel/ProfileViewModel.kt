package com.thesis.lumine.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import com.thesis.lumine.data.model.AdminUserInfo
import com.thesis.lumine.data.model.UpdateProfileRequest
import com.thesis.lumine.data.model.UserProfileResponse
import com.thesis.lumine.data.repository.JewelryRepository
import com.thesis.lumine.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository    = JewelryRepository()
    private val sessionManager = SessionManager(application.applicationContext)

    private val _profile = MutableStateFlow(UserProfileResponse())
    val profile: StateFlow<UserProfileResponse> = _profile

    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Admin
    private val _adminUsers = MutableStateFlow<List<AdminUserInfo>>(emptyList())
    val adminUsers: StateFlow<List<AdminUserInfo>> = _adminUsers

    private val _selectedUser = MutableStateFlow<AdminUserInfo?>(null)
    val selectedUser: StateFlow<AdminUserInfo?> = _selectedUser

    private val _userFavoriteIds = MutableStateFlow<List<String>>(emptyList())
    val userFavoriteIds: StateFlow<List<String>> = _userFavoriteIds

    private val _deleteSuccess = MutableStateFlow(false)
    val deleteSuccess: StateFlow<Boolean> = _deleteSuccess

    // i-load yung profile data ng user galing sa backend
    fun loadProfile() {
        val userId = sessionManager.getUserId() ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val resp = repository.getProfile(userId)
                if (resp.isSuccessful && resp.body() != null)
                    _profile.value = resp.body()!!
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // i-update yung profile info — blank mobile number treated as null
    fun updateProfile(username: String, firstName: String, lastName: String, mobileNumber: String?) {
        val userId = sessionManager.getUserId() ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val resp = repository.updateProfile(
                    userId,
                    UpdateProfileRequest(username, firstName, lastName, mobileNumber?.ifBlank { null })
                )
                if (resp.isSuccessful && resp.body() != null) {
                    _profile.value = resp.body()!!
                    _saveSuccess.value = true
                } else {
                    _error.value = "Failed to save profile"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // kunin yung list ng favorite jewelry IDs ng user tapos i-store as Set
    fun loadFavorites() {
        val userId = sessionManager.getUserId() ?: return
        viewModelScope.launch {
            try {
                val resp = repository.getFavorites(userId)
                if (resp.isSuccessful && resp.body() != null)
                    _favoriteIds.value = resp.body()!!.toSet()
            } catch (_: Exception) {}
        }
    }

    // i-toggle yung favorite — remove kung nandoon na, add kung wala pa
    fun toggleFavorite(jewelryId: String) {
        val userId = sessionManager.getUserId() ?: return
        viewModelScope.launch {
            if (favoriteIds.value.contains(jewelryId)) {
                repository.removeFavorite(userId, jewelryId)
                _favoriteIds.value = _favoriteIds.value - jewelryId
            } else {
                repository.addFavorite(userId, jewelryId)
                _favoriteIds.value = _favoriteIds.value + jewelryId
            }
        }
    }

    // i-read yung image bytes galing sa URI tapos i-upload bilang multipart
    fun uploadAvatar(uri: Uri) {
        val userId = sessionManager.getUserId() ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val context = getApplication<Application>().applicationContext
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val bytes = inputStream.readBytes()
                inputStream.close()
                val requestBody = bytes.toRequestBody("image/*".toMediaType())
                val part = MultipartBody.Part.createFormData("file", "avatar.jpg", requestBody)
                val resp = repository.uploadAvatar(userId, part)
                if (resp.isSuccessful && resp.body() != null) {
                    // i-update lang yung avatarUrl sa state, hindi buong profile
                    _profile.value = _profile.value.copy(avatarUrl = resp.body()!!.avatarUrl)
                } else {
                    _error.value = "Failed to upload photo"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // i-reset yung save flag para hindi mag-trigger ulit ang navigation
    fun resetSaveSuccess() { _saveSuccess.value = false }
    fun clearError()       { _error.value = null }

    // ── Admin — mga function na para lang sa admin user ────────────────────────

    // kunin lahat ng registered users para ipakita sa admin panel
    fun loadAdminUsers() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val resp = repository.getAdminUsers()
                if (resp.isSuccessful && resp.body() != null)
                    _adminUsers.value = resp.body()!!
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // i-set yung selected user para makita yung details niya
    fun selectUser(user: AdminUserInfo) {
        _selectedUser.value = user
    }

    // kunin yung favorites ng specific user — para sa admin view
    fun loadUserFavorites(userId: String) {
        viewModelScope.launch {
            try {
                val resp = repository.getUserFavorites(userId)
                if (resp.isSuccessful && resp.body() != null)
                    _userFavoriteIds.value = resp.body()!!
            } catch (_: Exception) {}
        }
    }

    // i-delete yung user account tapos i-update yung local list
    fun deleteUser(userId: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val resp = repository.deleteUser(userId)
                if (resp.isSuccessful) {
                    _deleteSuccess.value = true
                    // i-filter out yung deleted user sa local list para mag-update agad ang UI
                    _adminUsers.value = _adminUsers.value.filter { it.userId != userId }
                    onDone()
                } else {
                    _error.value = "Failed to remove account"
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // i-reset yung delete flag at i-clear yung selected user after deletion
    fun resetDeleteSuccess() { _deleteSuccess.value = false }
    fun clearSelectedUser()  { _selectedUser.value = null }
}
