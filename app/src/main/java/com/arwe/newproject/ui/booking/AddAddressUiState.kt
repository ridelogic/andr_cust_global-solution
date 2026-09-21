package com.arwe.newproject.ui.booking

import com.arwe.newproject.data.remote.dto.CustomerAddressResponse

sealed class AddAddressUiState {
    object Idle : AddAddressUiState()
    object Loading : AddAddressUiState()
    data class Success(val address: CustomerAddressResponse) : AddAddressUiState()
    data class Error(val message: String) : AddAddressUiState()
}
