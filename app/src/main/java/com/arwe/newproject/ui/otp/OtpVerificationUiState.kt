package com.arwe.newproject.ui.otp

import com.arwe.newproject.data.remote.dto.CustomerDto

sealed class OtpVerificationUiState {
    object Idle : OtpVerificationUiState()
    object Loading : OtpVerificationUiState()
    data class ExistingCustomerSuccess(val token: String, val customer: CustomerDto) : OtpVerificationUiState()
    data class RegistrationRequired(val mobile: String, val registrationToken: String) : OtpVerificationUiState()
    data class Error(val message: String) : OtpVerificationUiState()
}
