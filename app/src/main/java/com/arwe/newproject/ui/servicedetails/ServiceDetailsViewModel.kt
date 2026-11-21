package com.arwe.newproject.ui.servicedetails

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arwe.newproject.R
import com.arwe.newproject.data.repository.ApiErrorReason
import com.arwe.newproject.data.repository.CustomerAuthRepository
import com.arwe.newproject.data.repository.ServiceRequestDetailResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ServiceDetailsViewModel(
    application: Application,
    private val repository: CustomerAuthRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ServiceDetailsUiState>(ServiceDetailsUiState.Loading)
    val uiState: StateFlow<ServiceDetailsUiState> = _uiState.asStateFlow()

    /** Talks to GET customer/service-requests/{id} via the shared repository - see its own doc. */
    fun loadServiceRequestDetail(id: Long) {
        viewModelScope.launch {
            _uiState.value = ServiceDetailsUiState.Loading
            when (val result = repository.getServiceRequestDetail(id)) {
                is ServiceRequestDetailResult.Success -> {
                    _uiState.value = ServiceDetailsUiState.Loaded(result.response)
                }

                is ServiceRequestDetailResult.Error -> {
                    _uiState.value = ServiceDetailsUiState.Error(messageFor(result))
                }
            }
        }
    }

    private fun messageFor(error: ServiceRequestDetailResult.Error): String {
        val app = getApplication<Application>()
        val fieldMessage = error.fieldErrors?.values?.firstOrNull()?.firstOrNull()
        return fieldMessage ?: error.message ?: app.getString(fallbackServiceDetailsMessageResFor(error.reason))
    }
}

internal fun fallbackServiceDetailsMessageResFor(reason: ApiErrorReason): Int = when (reason) {
    ApiErrorReason.NOT_FOUND -> R.string.service_details_error_not_found
    ApiErrorReason.FORBIDDEN -> R.string.service_details_error_forbidden
    ApiErrorReason.UNAUTHORIZED -> R.string.service_details_error_unauthorized
    ApiErrorReason.TOO_MANY_ATTEMPTS -> R.string.add_address_error_too_many_attempts
    ApiErrorReason.SERVER_ERROR -> R.string.add_address_error_server
    ApiErrorReason.NETWORK_UNAVAILABLE -> R.string.add_address_error_network_unavailable
    ApiErrorReason.TIMEOUT -> R.string.add_address_error_timeout
    ApiErrorReason.VALIDATION_FAILED,
    ApiErrorReason.UNKNOWN -> R.string.service_details_error_generic
}
