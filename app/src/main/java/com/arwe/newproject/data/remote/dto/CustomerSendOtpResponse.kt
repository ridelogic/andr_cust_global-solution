package com.arwe.newproject.data.remote.dto

/**
 * Exact response body for POST customer/send-otp - see AuthController::sendOtp() in the Laravel
 * source. "otp" is returned in plain text ONLY because this endpoint is in its
 * development/testing phase (see that method's doc); it must never be logged or persisted on the
 * Android side - kept only in memory for the OTP verification screen's auto-fill convenience.
 */
data class CustomerSendOtpResponse(
    val message: String,
    val otp: String
)
