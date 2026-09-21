package com.arwe.newproject.data.repository

import com.arwe.newproject.data.remote.dto.ProductBatteryTypeResponse

sealed class ProductBatteryTypeListResult {
    data class Success(val batteryTypes: List<ProductBatteryTypeResponse>) : ProductBatteryTypeListResult()
    data class Error(
        val reason: ApiErrorReason,
        val message: String?,
        val fieldErrors: Map<String, List<String>>? = null
    ) : ProductBatteryTypeListResult()
}
