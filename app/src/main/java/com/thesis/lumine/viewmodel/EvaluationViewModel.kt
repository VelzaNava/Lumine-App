package com.thesis.lumine.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thesis.lumine.data.model.EvaluationRequest
import com.thesis.lumine.data.model.EvaluationSummary
import com.thesis.lumine.data.repository.JewelryRepository
import com.thesis.lumine.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EvaluationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository     = JewelryRepository()
    private val sessionManager = SessionManager(application.applicationContext)

    private val _submitted = MutableStateFlow(false)
    val submitted: StateFlow<Boolean> = _submitted

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _summaries = MutableStateFlow<List<EvaluationSummary>>(emptyList())
    val summaries: StateFlow<List<EvaluationSummary>> = _summaries

    // i-submit yung rating ng user para sa jewelry na na-try on niya
    fun submitRating(jewelryId: String, jewelryName: String, rating: Int, comment: String?) {
        val userId = sessionManager.getUserId() ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // i-build yung request object — blank comment treated as null
                val request = EvaluationRequest(
                    userId      = userId,
                    jewelryId   = jewelryId,
                    jewelryName = jewelryName,
                    rating      = rating,
                    comment     = comment?.ifBlank { null }
                )
                val resp = repository.submitEvaluation(request)
                if (resp.isSuccessful) _submitted.value = true
                else _error.value = "Failed to submit rating"
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    // kunin yung aggregated rating summaries para sa admin dashboard
    fun loadSummary() {
        viewModelScope.launch {
            try {
                val resp = repository.getEvaluationSummary()
                if (resp.isSuccessful && resp.body() != null)
                    _summaries.value = resp.body()!!
            } catch (_: Exception) {}
        }
    }

    // i-reset yung submitted flag para hindi mag-trigger ulit ang back navigation
    fun resetSubmitted() { _submitted.value = false }
    fun clearError()     { _error.value = null }
}
