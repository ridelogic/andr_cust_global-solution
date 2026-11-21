package com.arwe.newproject.ui.servicedetails

import com.arwe.newproject.data.remote.dto.CustomerServiceRequestDetailResponse

enum class ServiceTimelineStageState { COMPLETED, CURRENT, FUTURE }

/**
 * [timestamp] is only ever populated for the first stage (the ticket's real created_at) or a
 * fully-completed final stage (the ticket's real closed_at) - every other stage carries no
 * timestamp, since the detail API exposes no historical per-stage timestamps to source one from
 * (see ServiceRequestTimeline's own doc for why).
 */
data class ServiceTimelineStage(
    val label: String,
    val state: ServiceTimelineStageState,
    val timestamp: String? = null
)

/**
 * Builds a customer-facing progress timeline from POST/GET customer/service-requests(/{id})'s
 * response, using only real, confirmed backend concepts - never a fabricated date or an invented
 * stage. Two real, distinct backend vocabularies drive this (confirmed by reading the Laravel
 * source directly, C:\xampp\htdocs\global-services):
 *
 * 1. database\seeders\ServiceStatusSeeder.php - the ticket-level service_requests.status_id
 *    lifecycle. For a battery job, the ticket's own status.code directly walks through the
 *    battery pipeline (BATTERY_COLLECTED -> ... -> DELIVERED) - confirmed real codes, used as-is.
 *
 * 2. App\Models\TechnicianVisit's STATUS_* constants - a SEPARATE, per-visit status vocabulary
 *    (SCHEDULED/ACCEPTED/ON_THE_WAY/ARRIVED/IN_PROGRESS/COMPLETED/...). For a non-battery
 *    (appliance/general) job, the ticket-level status stays a generic code
 *    (NEW/ASSIGNED/ACCEPTED/...) while the *visit's* status is what actually reflects
 *    on-the-way/arrived/in-progress - this is why "Arrived" (which has no ticket-level
 *    ServiceStatus row at all) is sourced from the latest visit instead.
 *
 * The detail response has no status-history/per-stage-timestamp data at all (not eager-loaded,
 * not in ServiceRequestDetailResource::toArray()), so only real, always-present timestamps
 * (created_at, and closed_at once genuinely terminal-completed) are ever shown - see
 * ServiceTimelineStage's own doc.
 */
object ServiceRequestTimeline {

    // Confirmed directly from ServiceStatusSeeder.php - real codes/names/order, not invented.
    private val BATTERY_STAGES = listOf(
        "BATTERY_COLLECTED" to "Battery Collected",
        "BATTERY_RECEIVED" to "Battery Received",
        "INITIAL_INSPECTION" to "Initial Inspection",
        "INITIAL_TESTING" to "Initial Testing",
        "REGENERATION" to "Regeneration",
        "FINAL_TESTING" to "Final Testing",
        "READY_FOR_DELIVERY" to "Ready For Delivery",
        "DELIVERED" to "Delivered"
    )

    // Confirmed directly from TechnicianVisit::STATUS_* constants - real codes, not invented.
    private val VISIT_STAGE_CODES = listOf("ON_THE_WAY", "ARRIVED", "IN_PROGRESS", "COMPLETED")

    private const val TICKET_STATUS_COMPLETED = "COMPLETED"

    fun stagesFor(detail: CustomerServiceRequestDetailResponse): List<ServiceTimelineStage> {
        val batteryIndex = BATTERY_STAGES.indexOfFirst { it.first == detail.status.code }
        return if (batteryIndex >= 0) {
            batteryStages(batteryIndex)
        } else {
            applianceStages(detail)
        }
    }

    private fun batteryStages(currentIndex: Int): List<ServiceTimelineStage> =
        BATTERY_STAGES.mapIndexed { index, (_, label) ->
            ServiceTimelineStage(label = label, state = stateFor(index, currentIndex))
        }

    private fun applianceStages(detail: CustomerServiceRequestDetailResponse): List<ServiceTimelineStage> {
        val labels = listOf("Request Created", "Technician Assigned") + VISIT_STAGE_CODES.map(::visitStageLabel)
        val isTicketCompleted = detail.status.code == TICKET_STATUS_COMPLETED
        val latestVisitStatus = detail.visits?.firstOrNull()?.status
        val visitIndex = VISIT_STAGE_CODES.indexOfFirst { it == latestVisitStatus }

        val done = BooleanArray(labels.size)
        done[0] = true // Request Created - always true, this screen only exists for a request that was created
        done[1] = detail.technician != null
        when {
            isTicketCompleted -> for (i in 2 until labels.size) done[i] = true
            // The stage AT visitIndex is the visit's live status right now - CURRENT, not already
            // done - so only the stages strictly before it are marked completed.
            visitIndex >= 0 -> for (i in 0 until visitIndex) done[2 + i] = true
        }

        val currentIndex = done.indexOfFirst { !it }

        return labels.mapIndexed { index, label ->
            ServiceTimelineStage(
                label = label,
                state = when {
                    index == currentIndex -> ServiceTimelineStageState.CURRENT
                    done[index] -> ServiceTimelineStageState.COMPLETED
                    else -> ServiceTimelineStageState.FUTURE
                },
                timestamp = when {
                    index == 0 -> detail.createdAt
                    index == labels.lastIndex && done[index] -> detail.closedAt
                    else -> null
                }
            )
        }
    }

    private fun visitStageLabel(code: String): String = when (code) {
        "ON_THE_WAY" -> "On The Way"
        "ARRIVED" -> "Arrived"
        "IN_PROGRESS" -> "In Progress"
        "COMPLETED" -> "Completed"
        else -> code
    }

    private fun stateFor(index: Int, currentIndex: Int): ServiceTimelineStageState = when {
        index < currentIndex -> ServiceTimelineStageState.COMPLETED
        index == currentIndex -> ServiceTimelineStageState.CURRENT
        else -> ServiceTimelineStageState.FUTURE
    }
}
