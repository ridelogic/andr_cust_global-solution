package com.arwe.newproject.ui.booking

/**
 * Maps a selected Problem to its confirmed backend service_type_id for POST
 * customer/service-requests - see App\Models\ServiceType's BATTERY_REASON_CODES /
 * HOME_APPLIANCE_SERVICE_CODES doc and database\seeders\ServiceTypeSeeder.php in the Laravel
 * source (C:\xampp\htdocs\global-services), cross-checked directly against the local
 * service_types table (ids confirmed: 7=NOT_CHARGING, 8=LOW_BACKUP, 9=DEAD_BATTERY,
 * 10=REGENERATION_REQUIRED, 11=OTHER).
 *
 * Only BATTERY has a safe mapping here. For BATTERY, service_type_id genuinely *is* "the reason
 * the customer is bringing the battery in" - the same concept as this app's Problem step, so
 * Problem.id maps directly.
 *
 * For every other category (Home Appliance, Electrical, Plumbing, Other), the backend's
 * service_type is an *action* (Installation/Repair/Maintenance/Other - ServiceType::
 * HOME_APPLIANCE_SERVICE_CODES), not a problem description. Android's Home Appliance/Electrical/
 * Plumbing/Other categories currently only offer a generic "Not Working"/"Other" Problem (see
 * ProblemCatalog's `else` branch), and "Not Working" does not correspond to any of those action
 * codes - there is no ServiceType row for it. Guessing e.g. "Not Working" -> REPAIR would be
 * inventing an unconfirmed business rule and risks silently creating wrong bookings, so this
 * deliberately returns null for every non-BATTERY category until that mapping is actually
 * decided.
 */
fun serviceTypeIdFor(category: ServiceCategory, problem: Problem): Long? {
    if (category != ServiceCategory.BATTERY) return null

    return when (problem.id) {
        "not_charging" -> 7L
        "low_backup" -> 8L
        "dead_battery" -> 9L
        "regeneration_required" -> 10L
        "other" -> 11L
        else -> null
    }
}
