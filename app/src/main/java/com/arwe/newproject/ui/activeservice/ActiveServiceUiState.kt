package com.arwe.newproject.ui.activeservice

import com.arwe.newproject.data.remote.dto.CustomerServiceRequestSummaryResponse

sealed class ActiveServiceUiState {
    object Loading : ActiveServiceUiState()
    object Empty : ActiveServiceUiState()
    data class Loaded(val serviceRequests: List<CustomerServiceRequestSummaryResponse>) : ActiveServiceUiState()
    data class Error(val message: String) : ActiveServiceUiState()
}
