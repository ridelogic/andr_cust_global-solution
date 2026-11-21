package com.arwe.newproject.ui.servicedetails

import com.arwe.newproject.data.remote.dto.CustomerServiceRequestDetailResponse
import com.arwe.newproject.data.remote.dto.CustomerServiceRequestStatusResponse
import com.arwe.newproject.data.remote.dto.CustomerServiceRequestTechnicianResponse
import com.arwe.newproject.data.remote.dto.CustomerServiceRequestVisitResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Pure logic, no Android dependencies - see ServiceRequestTimeline's own doc for why each stage's
 * state/timestamp is derived the way it is.
 */
class ServiceRequestTimelineTest {

    private fun detail(
        statusCode: String,
        technician: CustomerServiceRequestTechnicianResponse? = null,
        visits: List<CustomerServiceRequestVisitResponse>? = null,
        createdAt: String? = "2026-09-20T10:00:00+00:00",
        closedAt: String? = null
    ) = CustomerServiceRequestDetailResponse(
        id = 1,
        ticketNumber = "GS0001",
        source = "MOBILE_APP",
        priority = "NORMAL",
        status = CustomerServiceRequestStatusResponse(code = statusCode, name = statusCode, isTerminal = false),
        serviceType = "Repair",
        complaint = "Not Working",
        customerNotes = null,
        preferredDate = "2026-09-25",
        preferredTimeFrom = "09:00",
        preferredTimeTo = null,
        createdAt = createdAt,
        closedAt = closedAt,
        address = null,
        product = null,
        technician = technician,
        visits = visits
    )

    private fun visit(status: String) = CustomerServiceRequestVisitResponse(
        visitNumber = 1, status = status, diagnosis = null, workPerformed = null,
        startedAt = null, endedAt = null, attachments = null
    )

    private fun technician() = CustomerServiceRequestTechnicianResponse(
        name = "Ravi Kumar", mobile = "9800000000", employeeCode = "EMP-001"
    )

    // --- Normal (non-battery) active status ---------------------------------------------------

    @Test
    fun `a brand-new request with no technician has Request Created completed and Technician Assigned current`() {
        val stages = ServiceRequestTimeline.stagesFor(detail(statusCode = "NEW"))

        assertEquals(
            listOf("Request Created", "Technician Assigned", "On The Way", "Arrived", "In Progress", "Completed"),
            stages.map { it.label }
        )
        assertEquals(ServiceTimelineStageState.COMPLETED, stages[0].state)
        assertEquals("2026-09-20T10:00:00+00:00", stages[0].timestamp)
        assertEquals(ServiceTimelineStageState.CURRENT, stages[1].state)
        stages.drop(2).forEach { assertEquals(ServiceTimelineStageState.FUTURE, it.state) }
    }

    @Test
    fun `a technician assigned with the latest visit ON_THE_WAY highlights On The Way as current, not completed`() {
        val stages = ServiceRequestTimeline.stagesFor(
            detail(statusCode = "ASSIGNED", technician = technician(), visits = listOf(visit("ON_THE_WAY")))
        )

        assertEquals(ServiceTimelineStageState.COMPLETED, stages[0].state) // Request Created
        assertEquals(ServiceTimelineStageState.COMPLETED, stages[1].state) // Technician Assigned
        assertEquals(ServiceTimelineStageState.CURRENT, stages[2].state)   // On The Way - happening right now
        assertEquals(ServiceTimelineStageState.FUTURE, stages[3].state)   // Arrived
        assertEquals(ServiceTimelineStageState.FUTURE, stages[4].state)   // In Progress
        assertEquals(ServiceTimelineStageState.FUTURE, stages[5].state)   // Completed
    }

    @Test
    fun `Arrived is sourced from the visit status, not any ticket-level status code`() {
        val stages = ServiceRequestTimeline.stagesFor(
            detail(statusCode = "IN_PROGRESS", technician = technician(), visits = listOf(visit("ARRIVED")))
        )

        assertEquals(ServiceTimelineStageState.COMPLETED, stages[2].state) // On The Way
        assertEquals(ServiceTimelineStageState.CURRENT, stages[3].state)   // Arrived
        assertEquals(ServiceTimelineStageState.FUTURE, stages[4].state)   // In Progress
    }

    @Test
    fun `no intermediate stage ever carries a fabricated timestamp`() {
        val stages = ServiceRequestTimeline.stagesFor(
            detail(statusCode = "IN_PROGRESS", technician = technician(), visits = listOf(visit("IN_PROGRESS")))
        )

        stages.drop(1).dropLast(1).forEach { assertNull(it.timestamp) }
    }

    // --- Completed status -----------------------------------------------------------------------

    @Test
    fun `a ticket-level COMPLETED status marks every appliance stage done and uses the real closed_at`() {
        val stages = ServiceRequestTimeline.stagesFor(
            detail(
                statusCode = "COMPLETED",
                technician = technician(),
                visits = listOf(visit("COMPLETED")),
                closedAt = "2026-09-22T15:30:00+00:00"
            )
        )

        stages.forEach { assertEquals(ServiceTimelineStageState.COMPLETED, it.state) }
        assertEquals("2026-09-22T15:30:00+00:00", stages.last().timestamp)
    }

    @Test
    fun `a completed ticket with no visits still marks all stages done via the ticket status alone`() {
        val stages = ServiceRequestTimeline.stagesFor(detail(statusCode = "COMPLETED", technician = technician()))

        stages.forEach { assertEquals(ServiceTimelineStageState.COMPLETED, it.state) }
    }

    // --- Battery status mapping ------------------------------------------------------------------

    @Test
    fun `a battery-pipeline status code is mapped onto the 8 battery stages, not the appliance ones`() {
        val stages = ServiceRequestTimeline.stagesFor(detail(statusCode = "REGENERATION"))

        assertEquals(
            listOf(
                "Battery Collected", "Battery Received", "Initial Inspection", "Initial Testing",
                "Regeneration", "Final Testing", "Ready For Delivery", "Delivered"
            ),
            stages.map { it.label }
        )
        assertEquals(ServiceTimelineStageState.COMPLETED, stages[0].state)
        assertEquals(ServiceTimelineStageState.COMPLETED, stages[3].state)
        assertEquals(ServiceTimelineStageState.CURRENT, stages[4].state) // Regeneration
        assertEquals(ServiceTimelineStageState.FUTURE, stages[5].state)
        assertEquals(ServiceTimelineStageState.FUTURE, stages[7].state)
    }

    @Test
    fun `the first battery stage BATTERY_COLLECTED is current with nothing completed before it`() {
        val stages = ServiceRequestTimeline.stagesFor(detail(statusCode = "BATTERY_COLLECTED"))

        assertEquals(ServiceTimelineStageState.CURRENT, stages[0].state)
        stages.drop(1).forEach { assertEquals(ServiceTimelineStageState.FUTURE, it.state) }
    }

    @Test
    fun `the final battery stage DELIVERED marks every battery stage completed`() {
        val stages = ServiceRequestTimeline.stagesFor(detail(statusCode = "DELIVERED"))

        stages.dropLast(1).forEach { assertEquals(ServiceTimelineStageState.COMPLETED, it.state) }
        assertEquals(ServiceTimelineStageState.CURRENT, stages.last().state)
    }

    @Test
    fun `battery stages never carry a timestamp, since no historical per-stage timestamps exist`() {
        val stages = ServiceRequestTimeline.stagesFor(detail(statusCode = "FINAL_TESTING"))

        stages.forEach { assertNull(it.timestamp) }
    }
}
