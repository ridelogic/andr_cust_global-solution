package com.arwe.newproject.data.remote.dto

/**
 * Exact response shape for GET customer/products/battery-types - see
 * App\Http\Controllers\Api\Customer\ProductController::batteryTypes() in the Laravel source
 * (C:\xampp\htdocs\global-services). Server-side already restricts this list to
 * ProductCategory::BATTERY_TYPE_CODES (CAR_BATTERY/BIKE_BATTERY/UPS_BATTERY/OTHER_BATTERY - the
 * "Battery Type Scope Lock"), so no Mobile Phone Battery (or any other category) can ever appear
 * here. "brands" is also present on the wire but omitted here - this screen only needs to know
 * which battery types exist, not their brands (a later brand/model picker would need it).
 */
data class ProductBatteryTypeResponse(
    val id: Long,
    val name: String,
    val code: String
)
