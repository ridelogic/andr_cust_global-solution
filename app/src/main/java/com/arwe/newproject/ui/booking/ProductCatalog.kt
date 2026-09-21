package com.arwe.newproject.ui.booking

import com.arwe.newproject.R

// Static placeholder catalog for every category except BATTERY, which is now driven by GET
// customer/products/battery-types (see ProductSelectionViewModel) - the four Product constants
// below are kept as the display source (title/description/icon) for that real data, matched by
// backend code via batteryProductForCode(), so the on-screen copy is unchanged either way.
object ProductCatalog {

    private val carBattery = Product(
        id = "car_battery",
        titleRes = R.string.product_car_battery_title,
        descriptionRes = R.string.product_car_battery_description,
        iconRes = R.drawable.ic_category_battery
    )
    private val bikeBattery = Product(
        id = "bike_battery",
        titleRes = R.string.product_bike_battery_title,
        descriptionRes = R.string.product_bike_battery_description,
        iconRes = R.drawable.ic_category_battery
    )
    private val upsBattery = Product(
        id = "ups_battery",
        titleRes = R.string.product_ups_battery_title,
        descriptionRes = R.string.product_ups_battery_description,
        iconRes = R.drawable.ic_category_battery
    )
    private val otherBattery = Product(
        id = "other_battery",
        titleRes = R.string.product_other_battery_title,
        descriptionRes = R.string.product_other_battery_description,
        iconRes = R.drawable.ic_category_battery
    )

    // Matches App\Models\ProductCategory::BATTERY_TYPE_CODES exactly - the backend's own "Battery
    // Type Scope Lock" already restricts the API to just these four, and this map additionally
    // ignores any code it doesn't recognize (batteryProductForCode returns null), so this app can
    // never render a fifth type such as Mobile Phone Battery even if that ever changed.
    private val batteryProductsByCode: Map<String, Product> = mapOf(
        "CAR_BATTERY" to carBattery,
        "BIKE_BATTERY" to bikeBattery,
        "UPS_BATTERY" to upsBattery,
        "OTHER_BATTERY" to otherBattery
    )

    fun batteryProductForCode(code: String): Product? = batteryProductsByCode[code.uppercase()]

    fun productsFor(category: ServiceCategory): List<Product> = when (category) {
        ServiceCategory.BATTERY -> listOf(carBattery, bikeBattery, upsBattery, otherBattery)

        ServiceCategory.HOME_APPLIANCE -> listOf(
            Product(
                id = "washing_machine",
                titleRes = R.string.product_washing_machine_title,
                descriptionRes = R.string.product_washing_machine_description,
                iconRes = R.drawable.ic_category_home_appliance
            ),
            Product(
                id = "refrigerator",
                titleRes = R.string.product_refrigerator_title,
                descriptionRes = R.string.product_refrigerator_description,
                iconRes = R.drawable.ic_category_home_appliance
            ),
            Product(
                id = "air_conditioner",
                titleRes = R.string.product_air_conditioner_title,
                descriptionRes = R.string.product_air_conditioner_description,
                iconRes = R.drawable.ic_category_home_appliance
            ),
            Product(
                id = "other_appliance",
                titleRes = R.string.product_other_appliance_title,
                descriptionRes = R.string.product_other_appliance_description,
                iconRes = R.drawable.ic_category_home_appliance
            )
        )

        ServiceCategory.ELECTRICAL -> listOf(
            Product(
                id = "wiring",
                titleRes = R.string.product_wiring_title,
                descriptionRes = R.string.product_wiring_description,
                iconRes = R.drawable.ic_category_electrical
            ),
            Product(
                id = "switchboard",
                titleRes = R.string.product_switchboard_title,
                descriptionRes = R.string.product_switchboard_description,
                iconRes = R.drawable.ic_category_electrical
            ),
            Product(
                id = "other_electrical",
                titleRes = R.string.product_other_electrical_title,
                descriptionRes = R.string.product_other_electrical_description,
                iconRes = R.drawable.ic_category_electrical
            )
        )

        ServiceCategory.PLUMBING -> listOf(
            Product(
                id = "pipe_leakage",
                titleRes = R.string.product_pipe_leakage_title,
                descriptionRes = R.string.product_pipe_leakage_description,
                iconRes = R.drawable.ic_category_plumbing
            ),
            Product(
                id = "tap_fitting",
                titleRes = R.string.product_tap_fitting_title,
                descriptionRes = R.string.product_tap_fitting_description,
                iconRes = R.drawable.ic_category_plumbing
            ),
            Product(
                id = "other_plumbing",
                titleRes = R.string.product_other_plumbing_title,
                descriptionRes = R.string.product_other_plumbing_description,
                iconRes = R.drawable.ic_category_plumbing
            )
        )

        ServiceCategory.OTHER -> listOf(
            Product(
                id = "general_service",
                titleRes = R.string.product_general_service_title,
                descriptionRes = R.string.product_general_service_description,
                iconRes = R.drawable.ic_category_other
            )
        )
    }
}
