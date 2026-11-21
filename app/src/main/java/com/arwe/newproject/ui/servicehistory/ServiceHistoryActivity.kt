package com.arwe.newproject.ui.servicehistory

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.arwe.newproject.R
import com.arwe.newproject.data.remote.dto.CustomerServiceRequestSummaryResponse
import com.arwe.newproject.databinding.ActivityServiceHistoryBinding
import com.arwe.newproject.databinding.ItemServiceRequestBinding
import com.arwe.newproject.ui.servicedetails.ServiceDetailsActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Shows the customer's terminal/completed service requests. Reuses GET
 * customer/service-requests and its DTOs/repository unchanged (same call as My Services/Active
 * Service) - "history" is simply that same list filtered to status.is_terminal == true (the
 * inverse of ActiveServiceViewModel's own filter - see its doc). Cards reuse
 * item_service_request.xml as-is - same fields, same card style as My Services/Active Service.
 */
class ServiceHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServiceHistoryBinding
    private val viewModel: ServiceHistoryViewModel by viewModels { ServiceHistoryViewModelFactory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServiceHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadServiceHistory()
    }

    private fun render(state: ServiceHistoryUiState) {
        binding.serviceHistoryProgress.visibility = View.GONE
        binding.serviceHistoryEmptyState.visibility = View.GONE
        binding.serviceHistoryContainer.visibility = View.GONE

        when (state) {
            is ServiceHistoryUiState.Loading -> {
                binding.serviceHistoryProgress.visibility = View.VISIBLE
            }

            is ServiceHistoryUiState.Empty -> {
                binding.serviceHistoryEmptyState.visibility = View.VISIBLE
            }

            is ServiceHistoryUiState.Loaded -> {
                binding.serviceHistoryContainer.visibility = View.VISIBLE
                bindServiceRequestList(state.serviceRequests)
            }

            is ServiceHistoryUiState.Error -> {
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun bindServiceRequestList(serviceRequests: List<CustomerServiceRequestSummaryResponse>) {
        binding.serviceHistoryContainer.removeAllViews()

        serviceRequests.forEachIndexed { index, serviceRequest ->
            val item = ItemServiceRequestBinding.inflate(layoutInflater, binding.serviceHistoryContainer, false)
            item.serviceRequestType.text = serviceRequest.serviceType
            item.serviceRequestStatus.text = serviceRequest.status.name
            item.serviceRequestTicket.text = serviceRequest.ticketNumber.orEmpty()
            item.serviceRequestDateTime.text = formatSchedule(serviceRequest)
            item.root.contentDescription = "${serviceRequest.serviceType}. ${serviceRequest.status.name}"
            item.root.setOnClickListener {
                startActivity(
                    Intent(this, ServiceDetailsActivity::class.java)
                        .putExtra(ServiceDetailsActivity.EXTRA_SERVICE_REQUEST_ID, serviceRequest.id)
                )
            }

            if (index > 0) {
                val params = item.root.layoutParams as ViewGroup.MarginLayoutParams
                params.topMargin = resources.getDimensionPixelSize(R.dimen.home_quick_card_spacing)
                item.root.layoutParams = params
            }

            binding.serviceHistoryContainer.addView(item.root)
        }
    }

    private fun formatSchedule(serviceRequest: CustomerServiceRequestSummaryResponse): String {
        val date = serviceRequest.preferredDate?.let { formatDate(it) }
        val time = serviceRequest.preferredTimeFrom?.let { formatTime(it) }

        return when {
            date != null && time != null -> getString(R.string.my_services_date_time_format, date, time)
            date != null -> getString(R.string.my_services_date_only_format, date)
            else -> getString(R.string.my_services_no_preferred_schedule)
        }
    }

    private fun formatDate(iso: String): String {
        val date = runCatching { LocalDate.parse(iso) }.getOrNull() ?: return iso
        return date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
    }

    private fun formatTime(value: String): String {
        val time = runCatching { LocalTime.parse(value) }.getOrNull() ?: return value
        return time.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))
    }
}
