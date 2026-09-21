package com.arwe.newproject.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Raw wire shape for POST customer/verify-otp's 200 response - see
 * AuthController::verifyOtp() in the Laravel source. That single endpoint returns one of two
 * genuinely different JSON shapes on the same HTTP 200:
 *
 * 1. Existing customer: {"token": "...", "customer": {...}}
 * 2. New mobile: {"requires_registration": true, "mobile": "...", "registration_token": "...",
 *    "message": "..."}
 *
 * Every field here is therefore nullable - this class is intentionally just the flat union of
 * both shapes as Gson sees them on the wire. Callers must not assume either shape; see
 * DefaultCustomerAuthRepository.verifyOtp(), which is the only place that interprets this DTO,
 * converting it into the unambiguous VerifyOtpResult sealed type via explicit null-checks (no
 * unsafe casts).
 */
data class CustomerVerifyOtpResponse(
    val token: String?,
    val customer: CustomerDto?,
    @SerializedName("requires_registration") val requiresRegistration: Boolean?,
    val mobile: String?,
    @SerializedName("registration_token") val registrationToken: String?,
    val message: String?
)
