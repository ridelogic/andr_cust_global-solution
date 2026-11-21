package com.arwe.newproject.ui.servicedetails

import com.arwe.newproject.data.remote.dto.CustomerServiceRequestDetailResponse

sealed class ServiceDetailsUiState {
    object Loading : ServiceDetailsUiState()
    data class Loaded(val detail: CustomerServiceRequestDetailResponse) : ServiceDetailsUiState()
    data class Error(val message: String) : ServiceDetailsUiState()
}
