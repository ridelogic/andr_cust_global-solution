package com.arwe.newproject.data.remote.dto

/**
 * Exact request body for POST customer/verify-otp - see
 * App\Http\Controllers\Api\Customer\AuthController::verifyOtp()'s $request->validate([...]) call
 * in the Laravel source (C:\xampp\htdocs\global-services). "device_name" is optional server-side
 * (nullable) and is omitted, same convention as every other auth request in this app.
 */
data class CustomerVerifyOtpRequest(
    val mobile: String,
    val otp: String
)
