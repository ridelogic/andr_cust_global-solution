package com.arwe.newproject.ui.booking

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arwe.newproject.R
import com.arwe.newproject.data.repository.AddressListResult
import com.arwe.newproject.data.repository.ApiErrorReason
import com.arwe.newproject.data.repository.CustomerAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddressSelectionViewModel(
    application: Application,
    private val repository: CustomerAuthRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<AddressListUiState>(AddressListUiState.Loading)
    val uiState: StateFlow<AddressListUiState> = _uiState.asStateFlow()

    init {
        loadAddresses()
    }

    /** Talks to GET customer/addresses via the shared repository - see its own doc. */
    fun loadAddresses() {
        viewModelScope.launch {
            _uiState.value = AddressListUiState.Loading
            when (val result = repository.getAddresses()) {
                is AddressListResult.Success -> {
                    _uiState.value = if (result.addresses.isEmpty()) {
                        AddressListUiState.Empty
                    } else {
                        AddressListUiState.Loaded(result.addresses)
                    }
                }

                is AddressListResult.Error -> {
                    _uiState.value = AddressListUiState.Error(messageFor(result))
                }
            }
        }
    }

    private fun messageFor(error: AddressListResult.Error): String {
        val app = getApplication<Application>()
        return error.message ?: app.getString(fallbackAddressListMessageResFor(error.reason))
    }
}

internal fun fallbackAddressListMessageResFor(reason: ApiErrorReason): Int = when (reason) {
    ApiErrorReason.UNAUTHORIZED -> R.string.address_list_error_unauthorized
    ApiErrorReason.TOO_MANY_ATTEMPTS -> R.string.add_address_error_too_many_attempts
    ApiErrorReason.SERVER_ERROR -> R.string.add_address_error_server
    ApiErrorReason.NETWORK_UNAVAILABLE -> R.string.add_address_error_network_unavailable
    ApiErrorReason.TIMEOUT -> R.string.add_address_error_timeout
    ApiErrorReason.FORBIDDEN,
    ApiErrorReason.NOT_FOUND,
    ApiErrorReason.VALIDATION_FAILED,
    ApiErrorReason.UNKNOWN -> R.string.address_list_error_generic
}
