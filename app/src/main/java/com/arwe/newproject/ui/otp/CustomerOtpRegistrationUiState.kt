package com.arwe.newproject.ui.otp

import com.arwe.newproject.data.remote.dto.CustomerDto

sealed class CustomerOtpRegistrationUiState {
    object Idle : CustomerOtpRegistrationUiState()
    object Loading : CustomerOtpRegistrationUiState()
    data class Success(val token: String, val customer: CustomerDto) : CustomerOtpRegistrationUiState()

    /** mobile/registrationToken (Intent extras, not user input) were blank - shouldn't happen via
     * normal navigation from OtpVerificationActivity, but the API must never be called without
     * them. No message string here on purpose: the Activity resolves it via getString(), same as
     * every other field-validation error in this app - keeps the ViewModel free of any Context
     * resource lookups of its own. */
    object MissingContext : CustomerOtpRegistrationUiState()

    data class Error(val message: String) : CustomerOtpRegistrationUiState()
}
