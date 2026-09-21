package com.arwe.newproject.ui.booking

import com.arwe.newproject.data.remote.dto.CustomerAddressResponse

sealed class AddressListUiState {
    object Loading : AddressListUiState()
    object Empty : AddressListUiState()
    data class Loaded(val addresses: List<CustomerAddressResponse>) : AddressListUiState()
    data class Error(val message: String) : AddressListUiState()
}
