package com.arwe.newproject.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Exact request body for POST customer/service-requests - see
 * App\Http\Controllers\Api\Customer\ServiceRequestController::store()'s $request->validate([...])
 * call in the Laravel source (C:\xampp\htdocs\global-services). service_type_id/
 * customer_address_id/complaint are required; the rest are nullable and omitted when unknown
 * (customer_product_id/product_model_id/serial_number/purchase_date are never sent - this app has
 * no product-registration UI yet, and the endpoint accepts a request with none of them).
 */
data class CustomerServiceRequestRequest(
    @SerializedName("service_type_id")
    val serviceTypeId: Long,

    @SerializedName("customer_address_id")
    val customerAddressId: Long,

    val complaint: String,

    @SerializedName("preferred_date")
    val preferredDate: String?,

    @SerializedName("preferred_time_from")
    val preferredTimeFrom: String?,

    @SerializedName("preferred_time_to")
    val preferredTimeTo: String?
)
