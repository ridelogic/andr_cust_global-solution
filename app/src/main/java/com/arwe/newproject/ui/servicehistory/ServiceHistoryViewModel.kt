package com.arwe.newproject.ui.servicehistory

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arwe.newproject.R
import com.arwe.newproject.data.repository.ApiErrorReason
import com.arwe.newproject.data.repository.CustomerAuthRepository
import com.arwe.newproject.data.repository.ServiceRequestListResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ServiceHistoryViewModel(
    application: Application,
    private val repository: CustomerAuthRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ServiceHistoryUiState>(ServiceHistoryUiState.Loading)
    val uiState: StateFlow<ServiceHistoryUiState> = _uiState.asStateFlow()

    /**
     * Reuses GET customer/service-requests unchanged (same repository call as My Services/Active
     * Service) - no duplicate networking/endpoint. "History" is the inverse of Active Service's
     * own definition: purely the backend's status.is_terminal flag (see ServiceStatusSeeder.php
     * in the Laravel source, C:\xampp\htdocs\global-services - only COMPLETED/CANCELLED/
     * SERVICE_NOT_POSSIBLE are is_terminal=true today, but this never hardcodes that list) -
     * status.is_terminal == true, never a hardcoded status-code whitelist.
     */
    fun loadServiceHistory() {
        viewModelScope.launch {
            _uiState.value = ServiceHistoryUiState.Loading
            when (val result = repository.getServiceRequests()) {
                is ServiceRequestListResult.Success -> {
                    val history = result.serviceRequests.filter { it.status.isTerminal }
                    _uiState.value = if (history.isEmpty()) {
                        ServiceHistoryUiState.Empty
                    } else {
                        ServiceHistoryUiState.Loaded(history)
                    }
                }

                is ServiceRequestListResult.Error -> {
                    _uiState.value = ServiceHistoryUiState.Error(messageFor(result))
                }
            }
        }
    }

    private fun messageFor(error: ServiceRequestListResult.Error): String {
        val app = getApplication<Application>()
        return error.message ?: app.getString(fallbackServiceHistoryMessageResFor(error.reason))
    }
}

internal fun fallbackServiceHistoryMessageResFor(reason: ApiErrorReason): Int = when (reason) {
    ApiErrorReason.UNAUTHORIZED -> R.string.service_history_error_unauthorized
    ApiErrorReason.TOO_MANY_ATTEMPTS -> R.string.add_address_error_too_many_attempts
    ApiErrorReason.SERVER_ERROR -> R.string.add_address_error_server
    ApiErrorReason.NETWORK_UNAVAILABLE -> R.string.add_address_error_network_unavailable
    ApiErrorReason.TIMEOUT -> R.string.add_address_error_timeout
    ApiErrorReason.FORBIDDEN,
    ApiErrorReason.NOT_FOUND,
    ApiErrorReason.VALIDATION_FAILED,
    ApiErrorReason.UNKNOWN -> R.string.service_history_error_generic
}
