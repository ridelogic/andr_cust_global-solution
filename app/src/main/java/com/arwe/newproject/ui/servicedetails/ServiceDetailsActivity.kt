package com.arwe.newproject.ui.servicedetails

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.arwe.newproject.R
import com.arwe.newproject.data.remote.dto.CustomerServiceRequestAddressResponse
import com.arwe.newproject.data.remote.dto.CustomerServiceRequestDetailResponse
import com.arwe.newproject.data.remote.dto.CustomerServiceRequestVisitResponse
import com.arwe.newproject.databinding.ActivityServiceDetailsBinding
import com.arwe.newproject.databinding.ItemSummaryBinding
import com.arwe.newproject.databinding.ItemTimelineStageBinding
import com.arwe.newproject.databinding.ItemVisitBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ServiceDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServiceDetailsBinding
    private val viewModel: ServiceDetailsViewModel by viewModels { ServiceDetailsViewModelFactory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServiceDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }

        val serviceRequestId = intent.getLongExtra(EXTRA_SERVICE_REQUEST_ID, -1L)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }

        if (serviceRequestId <= 0) {
            Snackbar.make(binding.root, R.string.service_details_error_generic, Snackbar.LENGTH_LONG).show()
        } else {
            viewModel.loadServiceRequestDetail(serviceRequestId)
        }
    }

    private fun render(state: ServiceDetailsUiState) {
        when (state) {
            is ServiceDetailsUiState.Loading -> {
                binding.serviceDetailsProgress.visibility = View.VISIBLE
                binding.serviceDetailsContent.visibility = View.GONE
            }

            is ServiceDetailsUiState.Loaded -> {
                binding.serviceDetailsProgress.visibility = View.GONE
                binding.serviceDetailsContent.visibility = View.VISIBLE
                bindDetail(state.detail)
            }

            is ServiceDetailsUiState.Error -> {
                binding.serviceDetailsProgress.visibility = View.GONE
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun bindDetail(detail: CustomerServiceRequestDetailResponse) {
        binding.ticketNumberText.text = detail.ticketNumber.orEmpty()
        binding.statusPillText.text = detail.status.name

        bindSummary(binding.summaryServiceType, R.drawable.ic_book_service, R.string.service_details_label_service_type, detail.serviceType)

        bindOptionalSummary(
            binding.summaryPreferredDate, R.drawable.ic_calendar, R.string.service_details_label_preferred_date,
            detail.preferredDate?.let(::formatDate)
        )
        bindOptionalSummary(
            binding.summaryPreferredTime, R.drawable.ic_active_service, R.string.service_details_label_preferred_time,
            detail.preferredTimeFrom?.let(::formatTime)
        )

        val product = detail.product
        binding.productSection.visibility = if (product != null) View.VISIBLE else View.GONE
        if (product != null) {
            bindSummary(binding.summaryProductCategory, R.drawable.ic_category_other, R.string.service_details_label_category, product.category)
            bindSummary(binding.summaryProductBrand, R.drawable.ic_category_other, R.string.service_details_label_brand, product.brand)
            bindSummary(binding.summaryProductModel, R.drawable.ic_category_other, R.string.service_details_label_model, product.modelName)
            bindOptionalSummaryRow(binding.summaryProductSerial, R.drawable.ic_category_other, R.string.service_details_label_serial_number, product.serialNumber)
        }

        binding.complaintText.text = detail.complaint

        val address = detail.address
        binding.addressSection.visibility = if (address != null) View.VISIBLE else View.GONE
        if (address != null) {
            binding.addressText.text = formatAddress(address)
        }

        val technician = detail.technician
        binding.technicianSection.visibility = if (technician != null) View.VISIBLE else View.GONE
        if (technician != null) {
            bindSummary(binding.summaryTechnicianName, R.drawable.ic_account, R.string.service_details_label_technician_name, technician.name)
            bindSummary(binding.summaryTechnicianMobile, R.drawable.ic_phone, R.string.service_details_label_technician_mobile, technician.mobile)
            bindOptionalSummaryRow(binding.summaryTechnicianEmployeeCode, R.drawable.ic_account, R.string.service_details_label_employee_code, technician.employeeCode)
        }

        bindTimeline(detail)
        bindVisits(detail.visits)
    }

    private fun bindTimeline(detail: CustomerServiceRequestDetailResponse) {
        binding.timelineContainer.removeAllViews()
        val stages = ServiceRequestTimeline.stagesFor(detail)

        stages.forEachIndexed { index, stage ->
            val item = ItemTimelineStageBinding.inflate(layoutInflater, binding.timelineContainer, false)
            item.timelineLabel.text = stage.label

            when (stage.state) {
                ServiceTimelineStageState.COMPLETED -> {
                    item.timelineDot.setImageResource(R.drawable.ic_check)
                    item.timelineDot.background = getDrawable(R.drawable.bg_timeline_dot)
                    item.timelineLabel.setTextColor(getColor(R.color.text_primary))
                }

                ServiceTimelineStageState.CURRENT -> {
                    item.timelineDot.setImageDrawable(null)
                    item.timelineDot.background = getDrawable(R.drawable.bg_timeline_dot_current)
                    item.timelineLabel.setTextColor(getColor(R.color.primary_blue))
                }

                ServiceTimelineStageState.FUTURE -> {
                    item.timelineDot.setImageDrawable(null)
                    item.timelineDot.background = getDrawable(R.drawable.bg_timeline_dot_future)
                    item.timelineLabel.setTextColor(getColor(R.color.text_secondary))
                }
            }

            if (index == stages.lastIndex) {
                item.timelineLine.visibility = View.INVISIBLE
            }

            val timestamp = stage.timestamp?.let(::formatDateTime)
            if (timestamp != null) {
                item.timelineTimestamp.visibility = View.VISIBLE
                item.timelineTimestamp.text = timestamp
            } else {
                item.timelineTimestamp.visibility = View.GONE
            }

            binding.timelineContainer.addView(item.root)
        }
    }

    private fun bindVisits(visits: List<CustomerServiceRequestVisitResponse>?) {
        binding.visitsContainer.removeAllViews()
        binding.visitsSection.visibility = if (visits.isNullOrEmpty()) View.GONE else View.VISIBLE
        if (visits.isNullOrEmpty()) return

        visits.forEachIndexed { index, visit ->
            val item = ItemVisitBinding.inflate(layoutInflater, binding.visitsContainer, false)
            item.visitTitle.text = getString(R.string.service_details_visit_title, visit.visitNumber, visit.status)

            val schedule = formatVisitSchedule(visit)
            if (schedule != null) {
                item.visitSchedule.visibility = View.VISIBLE
                item.visitSchedule.text = schedule
            } else {
                item.visitSchedule.visibility = View.GONE
            }

            if (!visit.diagnosis.isNullOrBlank()) {
                item.visitDiagnosis.visibility = View.VISIBLE
                item.visitDiagnosis.text = getString(R.string.service_details_visit_diagnosis, visit.diagnosis)
            } else {
                item.visitDiagnosis.visibility = View.GONE
            }

            if (!visit.workPerformed.isNullOrBlank()) {
                item.visitWorkPerformed.visibility = View.VISIBLE
                item.visitWorkPerformed.text = getString(R.string.service_details_visit_work_performed, visit.workPerformed)
            } else {
                item.visitWorkPerformed.visibility = View.GONE
            }

            val attachmentCount = visit.attachments?.size ?: 0
            if (attachmentCount > 0) {
                item.visitAttachments.visibility = View.VISIBLE
                item.visitAttachments.text = getString(R.string.service_details_visit_attachments_count, attachmentCount)
            } else {
                item.visitAttachments.visibility = View.GONE
            }

            if (index > 0) {
                val params = item.root.layoutParams as ViewGroup.MarginLayoutParams
                params.topMargin = resources.getDimensionPixelSize(R.dimen.home_quick_card_spacing)
                item.root.layoutParams = params
            }

            binding.visitsContainer.addView(item.root)
        }
    }

    private fun bindSummary(item: ItemSummaryBinding, iconRes: Int, labelRes: Int, value: String) {
        item.summaryIcon.setImageResource(iconRes)
        item.summaryLabel.setText(labelRes)
        item.summaryValue.text = value
        item.root.visibility = View.VISIBLE
    }

    /** Hides the whole row when [value] is null - used for optional top-level fields. */
    private fun bindOptionalSummary(item: ItemSummaryBinding, iconRes: Int, labelRes: Int, value: String?) {
        if (value == null) {
            item.root.visibility = View.GONE
        } else {
            bindSummary(item, iconRes, labelRes, value)
        }
    }

    private fun bindOptionalSummaryRow(item: ItemSummaryBinding, iconRes: Int, labelRes: Int, value: String?) =
        bindOptionalSummary(item, iconRes, labelRes, value?.takeIf { it.isNotBlank() })

    private fun formatAddress(address: CustomerServiceRequestAddressResponse): String {
        val line = if (address.addressLine2.isNullOrBlank()) {
            address.addressLine1
        } else {
            "${address.addressLine1}, ${address.addressLine2}"
        }
        val landmark = if (address.landmark.isNullOrBlank()) "" else ", ${address.landmark}"
        return "$line$landmark, ${address.city}, ${address.state} - ${address.postalCode}"
    }

    private fun formatVisitSchedule(visit: CustomerServiceRequestVisitResponse): String? {
        val started = visit.startedAt?.let(::formatDateTime) ?: return null
        val ended = visit.endedAt?.let(::formatDateTime)
        return if (ended != null) "$started – $ended" else started
    }

    private fun formatDate(iso: String): String {
        val date = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return iso
        return date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
    }

    private fun formatTime(value: String): String {
        val time = runCatching { LocalTime.parse(value) }.getOrNull() ?: return value
        return time.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))
    }

    private fun formatDateTime(iso: String): String {
        val dateTime = runCatching { OffsetDateTime.parse(iso) }.getOrNull() ?: return iso
        return dateTime.format(DateTimeFormatter.ofPattern("d MMM yyyy, hh:mm a", Locale.ENGLISH))
    }

    companion object {
        const val EXTRA_SERVICE_REQUEST_ID = "extra_service_request_id"
    }
}
