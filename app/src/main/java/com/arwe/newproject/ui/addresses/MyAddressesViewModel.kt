package com.arwe.newproject.ui.addresses

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arwe.newproject.R
import com.arwe.newproject.data.remote.dto.CustomerAddressResponse
import com.arwe.newproject.data.repository.AddressDeleteResult
import com.arwe.newproject.data.repository.AddressListResult
import com.arwe.newproject.data.repository.AddressResult
import com.arwe.newproject.data.repository.ApiErrorReason
import com.arwe.newproject.data.repository.CustomerAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyAddressesViewModel(
    application: Application,
    private val repository: CustomerAuthRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<MyAddressesUiState>(MyAddressesUiState.Loading)
    val uiState: StateFlow<MyAddressesUiState> = _uiState.asStateFlow()

    private val _actionState = MutableStateFlow<MyAddressesActionState>(MyAddressesActionState.Idle)
    val actionState: StateFlow<MyAddressesActionState> = _actionState.asStateFlow()

    /** Talks to GET customer/addresses via the shared repository - see its own doc. */
    fun loadAddresses() {
        viewModelScope.launch {
            _uiState.value = MyAddressesUiState.Loading
            when (val result = repository.getAddresses()) {
                is AddressListResult.Success -> {
                    _uiState.value = if (result.addresses.isEmpty()) {
                        MyAddressesUiState.Empty
                    } else {
                        MyAddressesUiState.Loaded(result.addresses)
                    }
                }

                is AddressListResult.Error -> {
                    _uiState.value = MyAddressesUiState.Error(messageForList(result))
                }
            }
        }
    }

    /** Talks to DELETE customer/addresses/{id} - reloads the list on success. */
    fun deleteAddress(id: Long) {
        viewModelScope.launch {
            _actionState.value = MyAddressesActionState.InProgress
            when (val result = repository.deleteAddress(id)) {
                is AddressDeleteResult.Success -> {
                    _actionState.value = MyAddressesActionState.Idle
                    loadAddresses()
                }

                is AddressDeleteResult.Error -> {
                    _actionState.value = MyAddressesActionState.Error(messageForDelete(result))
                }
            }
        }
    }

    /**
     * Talks to PATCH customer/addresses/{id} with every one of [address]'s own current field
     * values unchanged, only is_default forced to true - update() requires the full field set on
     * every call (see CustomerAuthRepository.updateAddress()'s own doc), and the backend
     * automatically un-defaults every other address server-side, so this never needs to touch any
     * other row itself. Reloads the list on success.
     */
    fun setDefault(address: CustomerAddressResponse) {
        viewModelScope.launch {
            _actionState.value = MyAddressesActionState.InProgress
            when (
                val result = repository.updateAddress(
                    id = address.id,
                    addressType = address.addressType,
                    addressLine1 = address.addressLine1,
                    addressLine2 = address.addressLine2,
                    landmark = address.landmark,
                    city = address.city,
                    state = address.state,
                    postalCode = address.postalCode,
                    isDefault = true
                )
            ) {
                is AddressResult.Success -> {
                    _actionState.value = MyAddressesActionState.Idle
                    loadAddresses()
                }

                is AddressResult.Error -> {
                    _actionState.value = MyAddressesActionState.Error(messageForUpdate(result))
                }
            }
        }
    }

    private fun messageForList(error: AddressListResult.Error): String {
        val app = getApplication<Application>()
        return error.message ?: app.getString(fallbackMyAddressesListMessageResFor(error.reason))
    }

    private fun messageForDelete(error: AddressDeleteResult.Error): String {
        val app = getApplication<Application>()
        return error.message ?: app.getString(fallbackMyAddressesActionMessageResFor(error.reason))
    }

    private fun messageForUpdate(error: AddressResult.Error): String {
        val app = getApplication<Application>()
        val fieldMessage = error.fieldErrors?.values?.firstOrNull()?.firstOrNull()
        return fieldMessage ?: error.message ?: app.getString(fallbackMyAddressesActionMessageResFor(error.reason))
    }
}

internal fun fallbackMyAddressesListMessageResFor(reason: ApiErrorReason): Int = when (reason) {
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

internal fun fallbackMyAddressesActionMessageResFor(reason: ApiErrorReason): Int = when (reason) {
    ApiErrorReason.UNAUTHORIZED -> R.string.address_list_error_unauthorized
    ApiErrorReason.TOO_MANY_ATTEMPTS -> R.string.add_address_error_too_many_attempts
    ApiErrorReason.SERVER_ERROR -> R.string.add_address_error_server
    ApiErrorReason.NETWORK_UNAVAILABLE -> R.string.add_address_error_network_unavailable
    ApiErrorReason.TIMEOUT -> R.string.add_address_error_timeout
    ApiErrorReason.VALIDATION_FAILED -> R.string.add_address_error_validation_generic
    ApiErrorReason.FORBIDDEN,
    ApiErrorReason.NOT_FOUND,
    ApiErrorReason.UNKNOWN -> R.string.my_addresses_error_action_generic
}
