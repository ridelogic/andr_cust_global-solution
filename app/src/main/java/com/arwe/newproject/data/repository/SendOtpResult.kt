package com.arwe.newproject.data.repository

import com.arwe.newproject.data.remote.dto.CustomerSendOtpResponse

sealed class SendOtpResult {
    data class Success(val response: CustomerSendOtpResponse) : SendOtpResult()
    data class Error(
        val reason: ApiErrorReason,
        val message: String?,
        val fieldErrors: Map<String, List<String>>? = null
    ) : SendOtpResult()
}
