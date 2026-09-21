package com.arwe.newproject.ui.booking

/**
 * [complaintValue] is the exact string this problem must carry forward as the "complaint" field
 * for POST customer/service-requests - see
 * App\Http\Controllers\Api\Customer\ServiceRequestController::store()'s
 * 'complaint' => ['required', 'string'] rule in the Laravel source
 * (C:\xampp\htdocs\global-services): any non-empty string is accepted, no enum/format
 * restriction, so this app's own problem title text is a valid value with no backend changes
 * needed. Deliberately a plain literal (not resolved via titleRes/Context.getString()) so the
 * backend value stays a fixed, locale-independent constant and is trivially unit-testable without
 * an Android Context - it happens to read identically to the current UI title text today.
 */
data class Problem(
    val id: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val iconRes: Int,
    val complaintValue: String
)
