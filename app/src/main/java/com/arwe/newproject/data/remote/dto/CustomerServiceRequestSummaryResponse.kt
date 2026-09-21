package com.arwe.newproject.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Matches App\Http\Resources\Customer\ServiceRequestSummaryResource::toArray() exactly - see the
 * Laravel source (C:\xampp\htdocs\global-services). Used by GET customer/service-requests (the
 * list endpoint). Deliberately has NO product/complaint/address fields - that resource simply
 * does not return them (only ServiceRequestDetailResource, the single-item endpoint, does); My
 * Services only shows what this summary actually provides.
 */
data class CustomerServiceRequestSummaryResponse(
    val id: Long,
    @SerializedName("ticket_number") val ticketNumber: String?,
    @SerializedName("service_type") val serviceType: String,
    val status: CustomerServiceRequestStatusResponse,
    val priority: String?,
    @SerializedName("preferred_date") val preferredDate: String?,
    @SerializedName("preferred_time_from") val preferredTimeFrom: String?,
    @SerializedName("preferred_time_to") val preferredTimeTo: String?,
    @SerializedName("created_at") val createdAt: String?
)
