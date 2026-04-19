package com.thesis.lumine.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thesis.lumine.data.model.Jewelry
import com.thesis.lumine.data.repository.JewelryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class JewelryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = JewelryRepository()

    private val _jewelryList = MutableStateFlow<List<Jewelry>>(emptyList())
    val jewelryList: StateFlow<List<Jewelry>> = _jewelryList

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _crudSuccess = MutableStateFlow<String?>(null)
    val crudSuccess: StateFlow<String?> = _crudSuccess

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    init {
        loadJewelry()
    }

    fun loadJewelry() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val response = repository.getAllJewelry()
                if (response.isSuccessful && response.body() != null) {
                    _jewelryList.value = response.body()!!
                } else {
                    _error.value = "Failed to load jewelry"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun filterByType(type: String) {
        viewModelScope.launch {
            try {
                val response = repository.getAllJewelry()
                if (response.isSuccessful && response.body() != null) {
                    _jewelryList.value = response.body()!!.filter {
                        it.type.equals(type, ignoreCase = true)
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun createJewelry(jewelry: Jewelry, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = repository.createJewelry(jewelry)
                if (response.isSuccessful) {
                    _crudSuccess.value = "${jewelry.name} added successfully."
                    loadJewelry()
                    onDone()
                } else {
                    _error.value = "Failed to add jewelry"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateJewelry(id: String, jewelry: Jewelry, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = repository.updateJewelry(id, jewelry)
                if (response.isSuccessful) {
                    _crudSuccess.value = "${jewelry.name} updated successfully."
                    loadJewelry()
                    onDone()
                } else {
                    _error.value = "Failed to update jewelry"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteJewelry(id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = repository.deleteJewelry(id)
                if (response.isSuccessful) {
                    _crudSuccess.value = "Item deleted."
                    loadJewelry()
                    onDone()
                } else {
                    _error.value = "Failed to delete jewelry"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Network error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // i-pick yung image galing sa phone storage tapos i-upload sa backend
    fun uploadJewelryImage(uri: Uri, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            try {
                _isUploading.value = true
                val context = getApplication<Application>().applicationContext
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val bytes = inputStream.readBytes()
                inputStream.close()
                val requestBody = bytes.toRequestBody("image/*".toMediaType())
                val part = MultipartBody.Part.createFormData("file", "jewelry.jpg", requestBody)
                val resp = repository.uploadJewelryImage(part)
                if (resp.isSuccessful && resp.body() != null) {
                    onSuccess(resp.body()!!.imageUrl)
                } else {
                    _error.value = "Failed to upload image"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Upload failed"
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun clearCrudSuccess() { _crudSuccess.value = null }
    fun clearError()       { _error.value = null }
}
