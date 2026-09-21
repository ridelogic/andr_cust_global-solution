package com.arwe.newproject.ui.myservices

import com.arwe.newproject.data.remote.dto.CustomerServiceRequestSummaryResponse

sealed class MyServicesUiState {
    object Loading : MyServicesUiState()
    object Empty : MyServicesUiState()
    data class Loaded(val serviceRequests: List<CustomerServiceRequestSummaryResponse>) : MyServicesUiState()
    data class Error(val message: String) : MyServicesUiState()
}
