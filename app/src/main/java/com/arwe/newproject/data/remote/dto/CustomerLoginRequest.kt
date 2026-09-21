package com.arwe.newproject.data.remote.dto

/**
 * Exact request body for POST customer/login - see
 * App\Http\Controllers\Api\Customer\AuthController::login()'s $request->validate([...]) call in
 * the Laravel source (C:\xampp\htdocs\global-services). Only "email" is accepted as the
 * identifier - this endpoint does not accept a mobile number, unlike registration's separate
 * "mobile" field. "device_name" is optional server-side (nullable) and is omitted, same
 * convention as CustomerRegisterRequest.
 */
data class CustomerLoginRequest(
    val email: String,
    val password: String
)
