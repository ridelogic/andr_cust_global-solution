package com.arwe.newproject.ui.booking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies the existing Problem Selection catalog is unchanged and that every problem carries a
 * non-blank complaintValue suitable for POST customer/service-requests' `'complaint' =>
 * ['required', 'string']` rule (see Problem.kt's doc) - this task does not wire up that POST
 * call, only confirms the model supports it.
 */
class ProblemCatalogTest {

    private val dummyProduct = Product(
        id = "car_battery",
        titleRes = 0,
        descriptionRes = 0,
        iconRes = 0
    )

    @Test
    fun `battery problems are exactly the five preserved problems, in order`() {
        val problems = ProblemCatalog.problemsFor(ServiceCategory.BATTERY, dummyProduct)

        assertEquals(
            listOf("not_charging", "low_backup", "dead_battery", "regeneration_required", "other"),
            problems.map { it.id }
        )
    }

    @Test
    fun `each battery problem's complaintValue matches its displayed title text exactly`() {
        val problems = ProblemCatalog.problemsFor(ServiceCategory.BATTERY, dummyProduct)
        val expected = mapOf(
            "not_charging" to "Not Charging",
            "low_backup" to "Low Backup",
            "dead_battery" to "Dead Battery",
            "regeneration_required" to "Regeneration Required",
            "other" to "Other"
        )

        problems.forEach { problem ->
            assertEquals(expected.getValue(problem.id), problem.complaintValue)
        }
    }

    @Test
    fun `no problem in any category has a blank complaintValue`() {
        val allProblems = ServiceCategory.values().flatMap { category ->
            ProblemCatalog.problemsFor(category, dummyProduct)
        }

        allProblems.forEach { problem ->
            assertFalse("complaintValue must not be blank for problem id=${problem.id}", problem.complaintValue.isBlank())
        }
    }

    @Test
    fun `resolving a selected problem id by the same pattern ConfirmBookingActivity uses yields a valid complaint value`() {
        // Mirrors ConfirmBookingActivity.resolveProblem(): look up by id, falling back to the
        // first entry - this is exactly how a future service-request submission would derive the
        // "complaint" field from the EXTRA_SELECTED_PROBLEM id carried through the booking flow.
        val problems = ProblemCatalog.problemsFor(ServiceCategory.BATTERY, dummyProduct)
        val selectedProblemId = "regeneration_required"

        val resolved = problems.firstOrNull { it.id == selectedProblemId } ?: problems.first()

        assertEquals("Regeneration Required", resolved.complaintValue)
        assertTrue(resolved.complaintValue.isNotBlank())
    }

    @Test
    fun `an unknown problem id falls back to the first problem, still producing a valid complaint value`() {
        val problems = ProblemCatalog.problemsFor(ServiceCategory.BATTERY, dummyProduct)

        val resolved = problems.firstOrNull { it.id == "not_a_real_id" } ?: problems.first()

        assertEquals("not_charging", resolved.id)
        assertEquals("Not Charging", resolved.complaintValue)
    }
}
