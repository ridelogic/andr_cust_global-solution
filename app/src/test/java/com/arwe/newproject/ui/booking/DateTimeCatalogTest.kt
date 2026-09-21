package com.arwe.newproject.ui.booking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Verifies the Date & Time Selection model against the Laravel POST customer/service-requests
 * contract (ServiceRequestController::store()) without integrating booking creation:
 * - preferred_date => ['nullable', 'date'] - zero-padded ISO yyyy-MM-dd.
 * - preferred_time_from/preferred_time_to => [..., 'date_format:H:i'] - zero-padded 24-hour
 *   HH:mm, which is what every TimeSlot.value already is.
 * Also covers the existing "no past dates" / "disable today's past time slots" scheduling rules,
 * pulled out of DateTimeSelectionActivity into testable pure functions with no behavior change.
 */
class DateTimeCatalogTest {

    @Test
    fun `preferredDateValue is zero-padded ISO yyyy-MM-dd, matching Laravel's date rule`() {
        assertEquals("2026-01-05", DateTimeCatalog.preferredDateValue(LocalDate.of(2026, 1, 5)))
        assertEquals("2026-09-20", DateTimeCatalog.preferredDateValue(LocalDate.of(2026, 9, 20)))
        assertEquals("2026-12-31", DateTimeCatalog.preferredDateValue(LocalDate.of(2026, 12, 31)))
    }

    @Test
    fun `every time slot value is zero-padded 24-hour HH-mm, matching Laravel's date_format H-i rule`() {
        val hhmmPattern = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

        DateTimeCatalog.timeSlots.forEach { slot ->
            assertTrue("slot value '${slot.value}' must match HH:mm", hhmmPattern.matches(slot.value))
        }

        // Spot-check the exact set (also documents there is no single "preferred_time" field on
        // the backend - only preferred_time_from/preferred_time_to - see DateTimeCatalog's doc).
        assertEquals(
            listOf("09:00", "10:00", "11:00", "12:00", "14:00", "15:00", "16:00", "17:00", "18:00"),
            DateTimeCatalog.timeSlots.map { it.value }
        )
    }

    @Test
    fun `today and future dates are selectable, past dates are not`() {
        val today = LocalDate.of(2026, 9, 20)

        assertTrue(DateTimeCatalog.isDateSelectable(today, today))
        assertTrue(DateTimeCatalog.isDateSelectable(today.plusDays(1), today))
        assertTrue(DateTimeCatalog.isDateSelectable(today.plusMonths(1), today))
        assertFalse(DateTimeCatalog.isDateSelectable(today.minusDays(1), today))
    }

    @Test
    fun `a time slot before the current time today is past, one after is not`() {
        val today = LocalDate.of(2026, 9, 20)
        val now = LocalTime.of(13, 30)
        val morningSlot = TimeSlot("09:00", "09:00 AM")
        val afternoonSlot = TimeSlot("14:00", "02:00 PM")

        assertTrue(DateTimeCatalog.isTimeSlotPast(morningSlot, referenceDate = today, today = today, now = now))
        assertFalse(DateTimeCatalog.isTimeSlotPast(afternoonSlot, referenceDate = today, today = today, now = now))
    }

    @Test
    fun `every slot is available for a future date regardless of clock time`() {
        val today = LocalDate.of(2026, 9, 20)
        val futureDate = today.plusDays(3)
        val now = LocalTime.of(23, 59)

        DateTimeCatalog.timeSlots.forEach { slot ->
            assertFalse(
                "slot ${slot.value} must not be past for a future date",
                DateTimeCatalog.isTimeSlotPast(slot, referenceDate = futureDate, today = today, now = now)
            )
        }
    }
}
