package com.arwe.newproject.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Matches App\Http\Resources\Customer\ServiceRequestDetailResource::toArray() exactly - see the
 * Laravel source (C:\xampp\htdocs\global-services). Used by both POST customer/service-requests
 * and GET customer/service-requests/{id}. "technician"/"visits" use Laravel's whenLoaded(), but
 * the controller's detailRelations() always eager-loads both, so on the wire they are always
 * present as either null/[] or real data - never guessed at, never fabricated when absent.
 */
data class CustomerServiceRequestDetailResponse(
    val id: Long,
    @SerializedName("ticket_number") val ticketNumber: String?,
    val source: String?,
    val priority: String?,
    val status: CustomerServiceRequestStatusResponse,
    @SerializedName("service_type") val serviceType: String,
    val complaint: String,
    @SerializedName("customer_notes") val customerNotes: String?,
    @SerializedName("preferred_date") val preferredDate: String?,
    @SerializedName("preferred_time_from") val preferredTimeFrom: String?,
    @SerializedName("preferred_time_to") val preferredTimeTo: String?,
    @SerializedName("created_at") val createdAt: String?,
    @SerializedName("closed_at") val closedAt: String?,
    val address: CustomerServiceRequestAddressResponse?,
    val product: CustomerServiceRequestProductResponse?,
    val technician: CustomerServiceRequestTechnicianResponse?,
    val visits: List<CustomerServiceRequestVisitResponse>?
)

/** Matches the "address" object exactly - built from CustomerAddress, not a shared AddressResource. */
data class CustomerServiceRequestAddressResponse(
    @SerializedName("address_line1") val addressLine1: String,
    @SerializedName("address_line2") val addressLine2: String?,
    val landmark: String?,
    val city: String,
    val state: String,
    @SerializedName("postal_code") val postalCode: String
)

/** Matches the "product" object exactly - category/brand/model_name come from the linked
 * ProductModel, not from CustomerProduct directly. */
data class CustomerServiceRequestProductResponse(
    val category: String,
    val brand: String,
    @SerializedName("model_name") val modelName: String,
    @SerializedName("serial_number") val serialNumber: String?
)

/** Matches the "technician" object exactly - deliberately limited to name/mobile/employee_code
 * (not email/alternate_mobile, which are staff-internal - see the Resource's own doc). */
data class CustomerServiceRequestTechnicianResponse(
    val name: String,
    val mobile: String,
    @SerializedName("employee_code") val employeeCode: String?
)

/** Matches one entry of the "visits" array exactly. */
data class CustomerServiceRequestVisitResponse(
    @SerializedName("visit_number") val visitNumber: Int,
    val status: String,
    val diagnosis: String?,
    @SerializedName("work_performed") val workPerformed: String?,
    @SerializedName("started_at") val startedAt: String?,
    @SerializedName("ended_at") val endedAt: String?,
    val attachments: List<CustomerServiceRequestAttachmentResponse>?
)

/** Matches one entry of a visit's "attachments" array exactly. */
data class CustomerServiceRequestAttachmentResponse(
    val id: Long,
    @SerializedName("file_type") val fileType: String,
    val url: String,
    val caption: String?
)
