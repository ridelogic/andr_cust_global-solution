package com.arwe.newproject.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Exact request body for POST customer/register-with-otp - see
 * App\Http\Controllers\Api\Customer\AuthController::registerWithOtp()'s $request->validate([...])
 * call in the Laravel source (C:\xampp\htdocs\global-services). No password/password_confirmation
 * field exists here - see RegisterCustomerWithOtpAction's doc for why this flow never collects
 * one. "device_name" is optional server-side (nullable) and is omitted, same convention as every
 * other auth request in this app.
 */
data class CustomerRegisterWithOtpRequest(
    val mobile: String,
    val name: String,
    val email: String?,
    @SerializedName("registration_token") val registrationToken: String
)
