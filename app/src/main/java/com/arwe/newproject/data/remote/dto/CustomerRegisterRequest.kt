package com.arwe.newproject.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Exact request body for POST customer/register - see
 * App\Http\Controllers\Api\Customer\AuthController::register()'s $request->validate([...]) call
 * in the Laravel source (C:\xampp\htdocs\global-services). "alternate_mobile" is deliberately NOT
 * a field here: that method never reads it, and App\Actions\Customer\RegisterCustomerAction::handle()
 * never sets it either - the Customer model has that column, but this endpoint does not accept it.
 * "device_name" is optional server-side (nullable) and is omitted entirely rather than guessed.
 */
data class CustomerRegisterRequest(
    val name: String,
    val mobile: String,
    val email: String,
    val password: String,
    @SerializedName("password_confirmation") val passwordConfirmation: String
)
