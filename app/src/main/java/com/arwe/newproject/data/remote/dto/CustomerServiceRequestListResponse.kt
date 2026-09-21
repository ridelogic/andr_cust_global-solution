package com.arwe.newproject.data.remote.dto

/**
 * Wire envelope for GET customer/service-requests - see
 * App\Http\Controllers\Api\Customer\ServiceRequestController::index() in the Laravel source.
 * Unlike AddressController::index() (which bypasses wrapping via response()->json(...)), this
 * method returns an AnonymousResourceCollection built from a paginator directly, so Laravel's
 * standard pagination envelope applies: {"data": [...], "links": {...}, "meta": {...}}. Only
 * "data" is modeled - pagination controls/"links"/"meta" are not used by this screen (the
 * backend already orders newest-first via ->latest()).
 */
data class CustomerServiceRequestListResponse(
    val data: List<CustomerServiceRequestSummaryResponse>
)
