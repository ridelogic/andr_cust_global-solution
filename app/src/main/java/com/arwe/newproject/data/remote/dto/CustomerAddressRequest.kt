package com.arwe.newproject.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Exact request body for POST customer/addresses - see
 * App\Http\Controllers\Api\Customer\AddressController::validated() in the Laravel source
 * (C:\xampp\htdocs\global-services). "is_default" is always sent explicitly, even though its
 * validation rule alone (['boolean']) would technically allow omitting it: store() does
 * `if ($validated['is_default'])` unconditionally, and an omitted field would leave that key
 * missing from Laravel's $validated array, causing an undefined-array-key error server-side.
 */
data class CustomerAddressRequest(
    @SerializedName("address_type") val addressType: String,
    @SerializedName("address_line1") val addressLine1: String,
    @SerializedName("address_line2") val addressLine2: String?,
    val landmark: String?,
    val city: String,
    val state: String,
    @SerializedName("postal_code") val postalCode: String,
    @SerializedName("is_default") val isDefault: Boolean
)
