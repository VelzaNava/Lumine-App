package com.thesis.lumine.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thesis.lumine.data.api.errorMessage
import com.thesis.lumine.data.api.toUserMessage
import com.thesis.lumine.data.model.EvaluationRequest
import com.thesis.lumine.data.model.EvaluationSummary
import com.thesis.lumine.data.repository.JewelryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Try-on ratings: submitting one after AR, and the admin's per-item summary. */
class EvaluationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = JewelryRepository()

    private val _submitted = MutableStateFlow(false)
    val submitted: StateFlow<Boolean> = _submitted

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _summaries = MutableStateFlow<List<EvaluationSummary>>(emptyList())
    val summaries: StateFlow<List<EvaluationSummary>> = _summaries

    /** Becomes true after the first summary load, so the tab can tell "loading" from "no ratings". */
    private val _summaryLoaded = MutableStateFlow(false)
    val summaryLoaded: StateFlow<Boolean> = _summaryLoaded

    /** Rates an item as the signed-in user; a blank comment is sent as none. */
    fun submitRating(jewelryId: String, jewelryName: String, rating: Int, comment: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val request = EvaluationRequest(
                    jewelryId = jewelryId,
                    jewelryName = jewelryName,
                    rating = rating,
                    comment = comment?.ifBlank { null }
                )
                val response = repository.submitEvaluation(request)
                if (response.isSuccessful) {
                    _submitted.value = true
                } else {
                    _error.value = response.errorMessage("Couldn't submit your rating. Please try again.")
                }
            } catch (e: Exception) {
                _error.value = e.toUserMessage()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Admin only: average rating per item. */
    fun loadSummary() {
        viewModelScope.launch {
            _error.value = null
            try {
                val response = repository.getEvaluationSummary()
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    _summaries.value = body
                } else {
                    _error.value = response.errorMessage("Couldn't load the ratings.")
                }
            } catch (e: Exception) {
                _error.value = e.toUserMessage()
            } finally {
                _summaryLoaded.value = true
            }
        }
    }

    /** Clears the flag so returning to the AR screen doesn't navigate away again. */
    fun resetSubmitted() {
        _submitted.value = false
    }

    fun clearError() {
        _error.value = null
    }
}
