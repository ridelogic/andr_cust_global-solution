package com.arwe.newproject.data.repository

import com.arwe.newproject.data.remote.dto.CustomerServiceRequestSummaryResponse

sealed class ServiceRequestListResult {
    data class Success(val serviceRequests: List<CustomerServiceRequestSummaryResponse>) : ServiceRequestListResult()
    data class Error(
        val reason: ApiErrorReason,
        val message: String?,
        val fieldErrors: Map<String, List<String>>? = null
    ) : ServiceRequestListResult()
}
