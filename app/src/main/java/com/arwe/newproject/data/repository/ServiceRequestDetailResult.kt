package com.arwe.newproject.data.repository

import com.arwe.newproject.data.remote.dto.CustomerServiceRequestDetailResponse

sealed class ServiceRequestDetailResult {
    data class Success(val response: CustomerServiceRequestDetailResponse) : ServiceRequestDetailResult()
    data class Error(
        val reason: ApiErrorReason,
        val message: String?,
        val fieldErrors: Map<String, List<String>>? = null
    ) : ServiceRequestDetailResult()
}
