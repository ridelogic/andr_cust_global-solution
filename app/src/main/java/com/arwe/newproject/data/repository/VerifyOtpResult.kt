package com.arwe.newproject.data.repository

import com.arwe.newproject.data.remote.dto.CustomerDto

/**
 * The unambiguous, already-disambiguated result of POST customer/verify-otp - see
 * DefaultCustomerAuthRepository.verifyOtp(), the only place that interprets the raw
 * CustomerVerifyOtpResponse wire shape into one of these three cases.
 */
sealed class VerifyOtpResult {
    data class ExistingCustomer(val token: String, val customer: CustomerDto) : VerifyOtpResult()
    data class RegistrationRequired(val mobile: String, val registrationToken: String) : VerifyOtpResult()
    data class Error(
        val reason: ApiErrorReason,
        val message: String?,
        val fieldErrors: Map<String, List<String>>? = null
    ) : VerifyOtpResult()
}
