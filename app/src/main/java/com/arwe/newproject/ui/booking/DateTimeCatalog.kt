package com.arwe.newproject.ui.booking

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

// Dates are derived from the device's current local date (never hardcoded);
// time slots are a fixed predefined list. No calendar or backend integration.
object DateTimeCatalog {

    fun next7Days(today: LocalDate = LocalDate.now()): List<DateSlot> =
        (0..6L).map { offset ->
            val date = today.plusDays(offset)
            DateSlot(
                isoDate = date.toString(),
                dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                dayNumber = date.dayOfMonth.toString(),
                month = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            )
        }

    val timeSlots: List<TimeSlot> = listOf(
        TimeSlot("09:00", "09:00 AM"),
        TimeSlot("10:00", "10:00 AM"),
        TimeSlot("11:00", "11:00 AM"),
        TimeSlot("12:00", "12:00 PM"),
        TimeSlot("14:00", "02:00 PM"),
        TimeSlot("15:00", "03:00 PM"),
        TimeSlot("16:00", "04:00 PM"),
        TimeSlot("17:00", "05:00 PM"),
        TimeSlot("18:00", "06:00 PM")
    )

    /**
     * ISO-8601 yyyy-MM-dd - matches
     * App\Http\Controllers\Api\Customer\ServiceRequestController::store()'s
     * 'preferred_date' => ['nullable', 'date'] rule exactly (see the Laravel source at
     * C:\xampp\htdocs\global-services). Booking creation is not integrated yet - this exists so
     * that a future task has a single, already-verified place this format comes from.
     */
    fun preferredDateValue(date: LocalDate): String = date.toString()

    /**
     * A date is selectable when it is today or any date after today - never a date before today.
     * Pulled out of DateTimeSelectionActivity's inline check so the "no past dates" rule is
     * independently testable without a View/Context.
     */
    fun isDateSelectable(date: LocalDate, today: LocalDate = LocalDate.now()): Boolean = !date.isBefore(today)

    /**
     * Whether [slot] has already passed for [referenceDate] (the selected/displayed date) - true
     * only when referenceDate is today AND the slot's own time is already before [now]. Any other
     * date leaves every slot enabled, matching DateTimeSelectionActivity's existing behavior
     * exactly (just pulled out into a testable pure function, not changed).
     *
     * slot.value is already zero-padded 24-hour HH:mm (e.g. "09:00", "14:00"), which matches
     * Laravel's 'preferred_time_from'/'preferred_time_to' => [..., 'date_format:H:i'] rule
     * exactly. Note there is no single "preferred_time" field on that endpoint - only a
     * from/to pair (with 'preferred_time_to' required to be after 'preferred_time_from') - and
     * this screen currently captures one time point per booking, so which of the two fields (or
     * both) a selected slot maps to is a decision for the future booking-integration task, not
     * this one.
     */
    fun isTimeSlotPast(
        slot: TimeSlot,
        referenceDate: LocalDate,
        today: LocalDate = LocalDate.now(),
        now: LocalTime = LocalTime.now()
    ): Boolean = referenceDate == today && LocalTime.parse(slot.value).isBefore(now)
}
