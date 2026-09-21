package com.arwe.newproject.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Partial response shape for POST customer/service-requests - see
 * ServiceRequestDetailResource::toArray() in the Laravel source. That resource returns a much
 * larger nested shape (status, address, product, technician, visits...), but this screen only
 * needs enough to confirm success and show a ticket reference - Gson simply ignores every other
 * field on the wire, so only id/ticket_number are modeled here.
 */
data class CustomerServiceRequestResponse(
    val id: Long,
    @SerializedName("ticket_number") val ticketNumber: String?
)
