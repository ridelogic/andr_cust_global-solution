package com.arwe.newproject.data.repository

import com.arwe.newproject.data.remote.dto.CustomerServiceRequestResponse

sealed class ServiceRequestResult {
    data class Success(val response: CustomerServiceRequestResponse) : ServiceRequestResult()
    data class Error(
        val reason: ApiErrorReason,
        val message: String?,
        val fieldErrors: Map<String, List<String>>? = null
    ) : ServiceRequestResult()
}
