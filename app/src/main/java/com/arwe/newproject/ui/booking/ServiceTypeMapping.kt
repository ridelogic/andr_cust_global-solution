package com.arwe.newproject.ui.booking

/**
 * Maps a selected Problem to its confirmed backend service_type_id for POST
 * customer/service-requests - see App\Models\ServiceType's BATTERY_REASON_CODES /
 * HOME_APPLIANCE_SERVICE_CODES doc and database\seeders\ServiceTypeSeeder.php in the Laravel
 * source (C:\xampp\htdocs\global-services), cross-checked directly against the local
 * service_types table (ids confirmed: 3=REPAIR, 7=NOT_CHARGING, 8=LOW_BACKUP, 9=DEAD_BATTERY,
 * 10=REGENERATION_REQUIRED, 11=OTHER).
 *
 * The app supports exactly two categories: BATTERY and HOME_APPLIANCE.
 *
 * For BATTERY, service_type_id genuinely *is* "the reason the customer is bringing the battery
 * in" - the same concept as this app's Problem step, so Problem.id maps directly.
 *
 * For HOME_APPLIANCE, the backend's service_type is an *action*
 * (Installation/Repair/Maintenance/Other - ServiceType::HOME_APPLIANCE_SERVICE_CODES), not a
 * problem description, so it does not map from Problem.id the same way BATTERY does. Both mappings
 * below are explicitly confirmed business decisions, not guesses: "Not Working" -> REPAIR, and
 * "Other" -> the shared generic catch-all OTHER (same service_type_id as BATTERY's own "Other"
 * problem above - it is a generic, category-agnostic ServiceType, not Home-Appliance-specific).
 */
fun serviceTypeIdFor(category: ServiceCategory, problem: Problem): Long? = when (category) {
    ServiceCategory.BATTERY -> when (problem.id) {
        "not_charging" -> 7L
        "low_backup" -> 8L
        "dead_battery" -> 9L
        "regeneration_required" -> 10L
        "other" -> 11L
        else -> null
    }

    ServiceCategory.HOME_APPLIANCE -> when (problem.id) {
        "not_working" -> 3L // REPAIR
        "other" -> 11L // OTHER - shared generic catch-all ServiceType
        else -> null
    }
}
