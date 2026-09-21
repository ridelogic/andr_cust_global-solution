package com.arwe.newproject.ui.myservices

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

class MyServicesViewModel(
    application: Application,
    private val repository: CustomerAuthRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<MyServicesUiState>(MyServicesUiState.Loading)
    val uiState: StateFlow<MyServicesUiState> = _uiState.asStateFlow()

    /** Talks to GET customer/service-requests via the shared repository - see its own doc. */
    fun loadServiceRequests() {
        viewModelScope.launch {
            _uiState.value = MyServicesUiState.Loading
            when (val result = repository.getServiceRequests()) {
                is ServiceRequestListResult.Success -> {
                    _uiState.value = if (result.serviceRequests.isEmpty()) {
                        MyServicesUiState.Empty
                    } else {
                        MyServicesUiState.Loaded(result.serviceRequests)
                    }
                }

                is ServiceRequestListResult.Error -> {
                    _uiState.value = MyServicesUiState.Error(messageFor(result))
                }
            }
        }
    }

    private fun messageFor(error: ServiceRequestListResult.Error): String {
        val app = getApplication<Application>()
        return error.message ?: app.getString(fallbackMyServicesMessageResFor(error.reason))
    }
}

internal fun fallbackMyServicesMessageResFor(reason: ApiErrorReason): Int = when (reason) {
    ApiErrorReason.UNAUTHORIZED -> R.string.my_services_error_unauthorized
    ApiErrorReason.TOO_MANY_ATTEMPTS -> R.string.add_address_error_too_many_attempts
    ApiErrorReason.SERVER_ERROR -> R.string.add_address_error_server
    ApiErrorReason.NETWORK_UNAVAILABLE -> R.string.add_address_error_network_unavailable
    ApiErrorReason.TIMEOUT -> R.string.add_address_error_timeout
    ApiErrorReason.FORBIDDEN,
    ApiErrorReason.NOT_FOUND,
    ApiErrorReason.VALIDATION_FAILED,
    ApiErrorReason.UNKNOWN -> R.string.my_services_error_generic
}
