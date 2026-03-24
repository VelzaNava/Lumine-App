package com.thesis.lumine.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thesis.lumine.data.model.Jewelry
import com.thesis.lumine.data.repository.JewelryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class JewelryViewModel : ViewModel() {

    private val repository = JewelryRepository()

    private val _jewelryList = MutableStateFlow<List<Jewelry>>(emptyList())
    val jewelryList: StateFlow<List<Jewelry>> = _jewelryList

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _crudSuccess = MutableStateFlow<String?>(null)
    val crudSuccess: StateFlow<String?> = _crudSuccess

    init {
        // i-load agad yung jewelry list pagka-init ng viewmodel
        loadJewelry()
    }

    // kunin lahat ng jewelry galing sa backend tapos i-store sa state
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

    // kunin lahat tapos i-filter sa client side base sa selected type
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

    // i-create yung bagong jewelry tapos i-reload yung list para mag-update ang UI
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

    // i-update yung existing jewelry — i-refresh yung list pagkatapos
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

    // i-delete yung jewelry item tapos i-reload para mawala sa list
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

    // i-clear yung success at error messages para hindi mag-show ulit
    fun clearCrudSuccess() { _crudSuccess.value = null }
    fun clearError()       { _error.value = null }
}
