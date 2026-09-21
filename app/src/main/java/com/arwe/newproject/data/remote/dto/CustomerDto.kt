package com.arwe.newproject.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Matches App\Http\Resources\Customer\CustomerResource::toArray() exactly (no "data" wrapper -
 * that Resource sets `public static $wrap = null`).
 */
data class CustomerDto(
    val id: Long,
    @SerializedName("customer_code") val customerCode: String?,
    val name: String,
    val mobile: String,
    @SerializedName("alternate_mobile") val alternateMobile: String?,
    val email: String?
)
