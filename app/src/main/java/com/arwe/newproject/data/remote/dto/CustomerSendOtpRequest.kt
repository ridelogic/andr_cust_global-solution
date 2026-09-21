package com.arwe.newproject.data.remote.dto

/**
 * Exact request body for POST customer/send-otp - see
 * App\Http\Controllers\Api\Customer\AuthController::sendOtp()'s $request->validate([...]) call in
 * the Laravel source (C:\xampp\htdocs\global-services). The backend only requires a non-empty
 * string; Android additionally validates format client-side using the same phone regex already
 * used by registration.
 */
data class CustomerSendOtpRequest(
    val mobile: String
)
