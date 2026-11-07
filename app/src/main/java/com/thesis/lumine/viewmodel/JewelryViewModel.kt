package com.thesis.lumine.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thesis.lumine.data.api.errorMessage
import com.thesis.lumine.data.api.toUserMessage
import com.thesis.lumine.data.model.Jewelry
import com.thesis.lumine.data.repository.JewelryRepository
import com.thesis.lumine.utils.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The jewelry catalog, shared by the customer screens and the admin panel.
 * Changes are applied to the in-memory list as soon as the server confirms
 * them, so lists update instantly without a reload or a spinner.
 */
class JewelryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = JewelryRepository()

    private val _jewelryList = MutableStateFlow<List<Jewelry>>(emptyList())
    val jewelryList: StateFlow<List<Jewelry>> = _jewelryList

    /**
     * True only while there is nothing on screen yet (first load, or after an
     * error left the list empty). Refreshing a populated list never sets it,
     * which is what keeps screens from flashing blank on every refresh.
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    /** True while a create/update/delete is in flight - for disabling that dialog's buttons. */
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /** One-shot confirmation text for a snackbar after a successful change. */
    private val _crudSuccess = MutableStateFlow<String?>(null)
    val crudSuccess: StateFlow<String?> = _crudSuccess

    init {
        loadJewelry()
    }

    /** Loads the catalog; a populated list refreshes silently in the background. */
    fun loadJewelry() {
        viewModelScope.launch {
            val firstLoad = _jewelryList.value.isEmpty()
            if (firstLoad) _isLoading.value = true
            _error.value = null
            try {
                val response = repository.getAllJewelry()
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    _jewelryList.value = body
                } else {
                    _error.value = response.errorMessage("Couldn't load the catalog.")
                }
            } catch (e: Exception) {
                _error.value = e.toUserMessage()
            } finally {
                if (firstLoad) _isLoading.value = false
            }
        }
    }

    /** Adds the server's copy (real id and timestamp) to the list - no refetch. */
    fun createJewelry(jewelry: Jewelry, onDone: () -> Unit) {
        runSaving {
            val response = repository.createJewelry(jewelry)
            val created = response.body()
            if (response.isSuccessful && created != null) {
                _jewelryList.value = _jewelryList.value + created
                _crudSuccess.value = "${jewelry.name} added."
                onDone()
            } else {
                _error.value = response.errorMessage("Couldn't add the item. Please try again.")
            }
        }
    }

    /** Swaps just the edited entry, so the list and its scroll position stay put. */
    fun updateJewelry(id: String, jewelry: Jewelry, onDone: () -> Unit) {
        runSaving {
            val response = repository.updateJewelry(id, jewelry)
            val updated = response.body()
            if (response.isSuccessful && updated != null) {
                _jewelryList.value = _jewelryList.value.map { if (it.id == id) updated else it }
                _crudSuccess.value = "${jewelry.name} updated."
                onDone()
            } else {
                _error.value = response.errorMessage("Couldn't save the changes. Please try again.")
            }
        }
    }

    fun deleteJewelry(id: String, onDone: () -> Unit) {
        runSaving {
            val response = repository.deleteJewelry(id)
            if (response.isSuccessful) {
                _jewelryList.value = _jewelryList.value.filterNot { it.id == id }
                _crudSuccess.value = "Item deleted."
                onDone()
            } else {
                _error.value = response.errorMessage("Couldn't delete the item. Please try again.")
            }
        }
    }

    /** Compresses the picked photo off the main thread, uploads it and returns its URL. */
    fun uploadJewelryImage(uri: Uri, onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _isUploading.value = true
            try {
                val context = getApplication<Application>().applicationContext
                val part = withContext(Dispatchers.IO) { ImageCompressor.toUploadPart(context, uri, "jewelry") }
                if (part == null) {
                    _error.value = "That photo couldn't be read. Please choose another one."
                    return@launch
                }

                val response = repository.uploadJewelryImage(part)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    onSuccess(body.imageUrl)
                } else {
                    _error.value = response.errorMessage("Couldn't upload the photo. Please try again.")
                }
            } catch (e: Exception) {
                _error.value = e.toUserMessage()
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun clearCrudSuccess() {
        _crudSuccess.value = null
    }

    fun clearError() {
        _error.value = null
    }

    private fun runSaving(block: suspend () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                block()
            } catch (e: Exception) {
                _error.value = e.toUserMessage()
            } finally {
                _isSaving.value = false
            }
        }
    }
}
