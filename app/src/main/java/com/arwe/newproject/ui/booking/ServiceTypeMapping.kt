package com.arwe.newproject.ui.booking

/**
 * Maps a selected Problem to its confirmed backend service_type_id for POST
 * customer/service-requests - see App\Models\ServiceType's BATTERY_REASON_CODES /
 * HOME_APPLIANCE_SERVICE_CODES doc and database\seeders\ServiceTypeSeeder.php in the Laravel
 * source (C:\xampp\htdocs\global-services), cross-checked directly against the local
 * service_types table (ids confirmed: 2=INSTALLATION, 3=REPAIR, 4=MAINTENANCE, 7=NOT_CHARGING,
 * 8=LOW_BACKUP, 9=DEAD_BATTERY, 10=REGENERATION_REQUIRED, 11=OTHER).
 *
 * For BATTERY, service_type_id genuinely *is* "the reason the customer is bringing the battery
 * in" - the same concept as this app's Problem step, so Problem.id maps directly.
 *
 * For HOME_APPLIANCE, the backend's service_type is an *action*
 * (Installation/Repair/Maintenance/Other - ServiceType::HOME_APPLIANCE_SERVICE_CODES), not a
 * problem description, so it does not map from Problem.id the same way BATTERY does. The one
 * mapping below (Home Appliance's "Not Working" -> REPAIR) is an explicitly confirmed business
 * decision, not a guess. The other Home Appliance problem ("Other") and every problem under
 * Electrical/Plumbing/Other are NOT mapped here - no equivalent decision has been confirmed for
 * them yet, so they deliberately still return null rather than invent one.
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
        else -> null
    }

    else -> null
}
