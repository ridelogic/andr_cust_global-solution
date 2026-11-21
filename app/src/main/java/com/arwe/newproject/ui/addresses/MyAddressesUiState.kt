package com.arwe.newproject.ui.addresses

import com.arwe.newproject.data.remote.dto.CustomerAddressResponse

/** Drives the address list itself (GET customer/addresses). */
sealed class MyAddressesUiState {
    object Loading : MyAddressesUiState()
    object Empty : MyAddressesUiState()
    data class Loaded(val addresses: List<CustomerAddressResponse>) : MyAddressesUiState()
    data class Error(val message: String) : MyAddressesUiState()
}

/**
 * Drives delete/set-default row actions separately from [MyAddressesUiState], so an in-flight
 * action shows its own progress/error without hiding the already-loaded list underneath it (a
 * shared state would force the list to disappear during every delete/set-default call).
 */
sealed class MyAddressesActionState {
    object Idle : MyAddressesActionState()
    object InProgress : MyAddressesActionState()
    data class Error(val message: String) : MyAddressesActionState()
}
