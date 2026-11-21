package com.arwe.newproject.ui.activeservice

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

class ActiveServiceViewModel(
    application: Application,
    private val repository: CustomerAuthRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ActiveServiceUiState>(ActiveServiceUiState.Loading)
    val uiState: StateFlow<ActiveServiceUiState> = _uiState.asStateFlow()

    /**
     * Reuses GET customer/service-requests unchanged (same repository call as My Services) - no
     * duplicate networking/endpoint. "Active" is defined purely by the backend's own
     * status.is_terminal flag (see ServiceStatusSeeder.php in the Laravel source,
     * C:\xampp\htdocs\global-services: only COMPLETED/CANCELLED/SERVICE_NOT_POSSIBLE are
     * is_terminal=true - every other status, including future ones the backend might add, is
     * "active") - never a hardcoded status-code whitelist.
     */
    fun loadActiveServiceRequests() {
        viewModelScope.launch {
            _uiState.value = ActiveServiceUiState.Loading
            when (val result = repository.getServiceRequests()) {
                is ServiceRequestListResult.Success -> {
                    val active = result.serviceRequests.filterNot { it.status.isTerminal }
                    _uiState.value = if (active.isEmpty()) {
                        ActiveServiceUiState.Empty
                    } else {
                        ActiveServiceUiState.Loaded(active)
                    }
                }

                is ServiceRequestListResult.Error -> {
                    _uiState.value = ActiveServiceUiState.Error(messageFor(result))
                }
            }
        }
    }

    private fun messageFor(error: ServiceRequestListResult.Error): String {
        val app = getApplication<Application>()
        return error.message ?: app.getString(fallbackActiveServiceMessageResFor(error.reason))
    }
}

internal fun fallbackActiveServiceMessageResFor(reason: ApiErrorReason): Int = when (reason) {
    ApiErrorReason.UNAUTHORIZED -> R.string.active_service_error_unauthorized
    ApiErrorReason.TOO_MANY_ATTEMPTS -> R.string.add_address_error_too_many_attempts
    ApiErrorReason.SERVER_ERROR -> R.string.add_address_error_server
    ApiErrorReason.NETWORK_UNAVAILABLE -> R.string.add_address_error_network_unavailable
    ApiErrorReason.TIMEOUT -> R.string.add_address_error_timeout
    ApiErrorReason.FORBIDDEN,
    ApiErrorReason.NOT_FOUND,
    ApiErrorReason.VALIDATION_FAILED,
    ApiErrorReason.UNKNOWN -> R.string.active_service_error_generic
}
