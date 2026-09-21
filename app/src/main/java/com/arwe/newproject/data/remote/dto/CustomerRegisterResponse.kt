package com.arwe.newproject.data.remote.dto

/**
 * Matches the HTTP 201 response from
 * App\Http\Controllers\Api\Customer\AuthController::register() exactly - see that method's
 * response()->json(['token' => ..., 'customer' => new CustomerResource($customer)], 201) call.
 */
data class CustomerRegisterResponse(
    val token: String,
    val customer: CustomerDto
)
