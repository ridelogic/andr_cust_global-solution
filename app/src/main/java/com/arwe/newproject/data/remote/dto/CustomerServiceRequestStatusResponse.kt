package com.arwe.newproject.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Matches the nested "status" object in ServiceRequestSummaryResource::toArray() exactly. */
data class CustomerServiceRequestStatusResponse(
    val code: String,
    val name: String,
    @SerializedName("is_terminal") val isTerminal: Boolean
)
