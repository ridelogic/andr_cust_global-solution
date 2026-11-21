package com.arwe.newproject.ui.servicehistory

import com.arwe.newproject.data.remote.dto.CustomerServiceRequestSummaryResponse

sealed class ServiceHistoryUiState {
    object Loading : ServiceHistoryUiState()
    object Empty : ServiceHistoryUiState()
    data class Loaded(val serviceRequests: List<CustomerServiceRequestSummaryResponse>) : ServiceHistoryUiState()
    data class Error(val message: String) : ServiceHistoryUiState()
}
