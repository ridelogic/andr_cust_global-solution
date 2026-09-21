package com.arwe.newproject.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Matches App\Http\Resources\Customer\AddressResource::toArray() exactly (no "data" wrapper -
 * that Resource sets `public static $wrap = null`). Used by both GET customer/addresses (a plain
 * JSON array of these, no pagination envelope - see AddressController::index()) and POST
 * customer/addresses (a single one, HTTP 200 - store() sets no explicit status code).
 * latitude/longitude are Laravel decimal:7 casts, which serialize as JSON strings, not numbers.
 */
data class CustomerAddressResponse(
    val id: Long,
    @SerializedName("address_type") val addressType: String,
    @SerializedName("address_line1") val addressLine1: String,
    @SerializedName("address_line2") val addressLine2: String?,
    val landmark: String?,
    val city: String,
    val state: String,
    @SerializedName("postal_code") val postalCode: String,
    val latitude: String?,
    val longitude: String?,
    @SerializedName("is_default") val isDefault: Boolean
)
