package com.arwe.newproject.ui.booking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ServiceTypeMappingTest {

    private val dummyProduct = Product(id = "car_battery", titleRes = 0, descriptionRes = 0, iconRes = 0)

    private fun batteryProblem(id: String): Problem =
        ProblemCatalog.problemsFor(ServiceCategory.BATTERY, dummyProduct).first { it.id == id }

    @Test
    fun `each of the five battery problems maps to its confirmed service_type_id`() {
        assertEquals(7L, serviceTypeIdFor(ServiceCategory.BATTERY, batteryProblem("not_charging")))
        assertEquals(8L, serviceTypeIdFor(ServiceCategory.BATTERY, batteryProblem("low_backup")))
        assertEquals(9L, serviceTypeIdFor(ServiceCategory.BATTERY, batteryProblem("dead_battery")))
        assertEquals(10L, serviceTypeIdFor(ServiceCategory.BATTERY, batteryProblem("regeneration_required")))
        assertEquals(11L, serviceTypeIdFor(ServiceCategory.BATTERY, batteryProblem("other")))
    }

    @Test
    fun `no non-BATTERY category has a safe mapping, regardless of the selected problem`() {
        val nonBatteryCategories = ServiceCategory.values().filter { it != ServiceCategory.BATTERY }

        nonBatteryCategories.forEach { category ->
            ProblemCatalog.problemsFor(category, dummyProduct).forEach { problem ->
                assertNull(
                    "category=$category problem=${problem.id} must not have a guessed service_type_id",
                    serviceTypeIdFor(category, problem)
                )
            }
        }
    }

    @Test
    fun `an unrecognized problem id under BATTERY also returns null rather than a guess`() {
        val unknownProblem = Problem(
            id = "not_a_real_problem",
            titleRes = 0,
            descriptionRes = 0,
            iconRes = 0,
            complaintValue = "Something Else"
        )

        assertNull(serviceTypeIdFor(ServiceCategory.BATTERY, unknownProblem))
    }
}
