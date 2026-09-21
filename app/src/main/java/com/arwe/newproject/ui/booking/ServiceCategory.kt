package com.arwe.newproject.ui.booking

import com.arwe.newproject.R

enum class ServiceCategory(
    val id: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val iconRes: Int
) {
    BATTERY(
        id = "battery",
        titleRes = R.string.category_battery_title,
        descriptionRes = R.string.category_battery_description,
        iconRes = R.drawable.ic_category_battery
    ),
    HOME_APPLIANCE(
        id = "home_appliance",
        titleRes = R.string.category_home_appliance_title,
        descriptionRes = R.string.category_home_appliance_description,
        iconRes = R.drawable.ic_category_home_appliance
    ),
    ELECTRICAL(
        id = "electrical",
        titleRes = R.string.category_electrical_title,
        descriptionRes = R.string.category_electrical_description,
        iconRes = R.drawable.ic_category_electrical
    ),
    PLUMBING(
        id = "plumbing",
        titleRes = R.string.category_plumbing_title,
        descriptionRes = R.string.category_plumbing_description,
        iconRes = R.drawable.ic_category_plumbing
    ),
    OTHER(
        id = "other",
        titleRes = R.string.category_other_title,
        descriptionRes = R.string.category_other_description,
        iconRes = R.drawable.ic_category_other
    )
}
