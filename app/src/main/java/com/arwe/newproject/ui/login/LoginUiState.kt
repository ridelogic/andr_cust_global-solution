package com.arwe.newproject.ui.login

sealed class LoginUiState {
    object Idle : LoginUiState()
    object Loading : LoginUiState()

    /**
     * "otp" is carried only in memory for this screen's brief hand-off to
     * OtpVerificationActivity (development/testing auto-fill convenience) - never logged, never
     * persisted.
     */
    data class Success(val mobile: String, val otp: String) : LoginUiState()
    data class Error(val message: String) : LoginUiState()
}
