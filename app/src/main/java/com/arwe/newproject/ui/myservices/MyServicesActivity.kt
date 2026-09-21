package com.arwe.newproject.ui.myservices

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.arwe.newproject.R
import com.arwe.newproject.data.remote.dto.CustomerServiceRequestSummaryResponse
import com.arwe.newproject.databinding.ActivityMyServicesBinding
import com.arwe.newproject.databinding.ItemServiceRequestBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Shows the customer's own service requests (GET customer/service-requests). Cards only display
 * fields ServiceRequestSummaryResource actually returns (service_type/status/preferred_date/
 * preferred_time_from/ticket_number) - that resource has no product/complaint/address fields;
 * only the detail endpoint does, and that screen is not implemented yet (see the "Details coming
 * soon" tap handler below).
 */
class MyServicesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyServicesBinding
    private val viewModel: MyServicesViewModel by viewModels { MyServicesViewModelFactory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyServicesBinding.inflate(layoutInflater)
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
        viewModel.loadServiceRequests()
    }

    private fun render(state: MyServicesUiState) {
        binding.myServicesProgress.visibility = View.GONE
        binding.myServicesEmptyState.visibility = View.GONE
        binding.serviceRequestContainer.visibility = View.GONE

        when (state) {
            is MyServicesUiState.Loading -> {
                binding.myServicesProgress.visibility = View.VISIBLE
            }

            is MyServicesUiState.Empty -> {
                binding.myServicesEmptyState.visibility = View.VISIBLE
            }

            is MyServicesUiState.Loaded -> {
                binding.serviceRequestContainer.visibility = View.VISIBLE
                bindServiceRequestList(state.serviceRequests)
            }

            is MyServicesUiState.Error -> {
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun bindServiceRequestList(serviceRequests: List<CustomerServiceRequestSummaryResponse>) {
        binding.serviceRequestContainer.removeAllViews()

        serviceRequests.forEachIndexed { index, serviceRequest ->
            val item = ItemServiceRequestBinding.inflate(layoutInflater, binding.serviceRequestContainer, false)
            item.serviceRequestType.text = serviceRequest.serviceType
            item.serviceRequestStatus.text = serviceRequest.status.name
            item.serviceRequestTicket.text = serviceRequest.ticketNumber.orEmpty()
            item.serviceRequestDateTime.text = formatSchedule(serviceRequest)
            item.root.contentDescription = "${serviceRequest.serviceType}. ${serviceRequest.status.name}"
            item.root.setOnClickListener {
                // Service Details is not implemented yet - see class doc.
                Toast.makeText(this, R.string.my_services_details_coming_soon, Toast.LENGTH_SHORT).show()
            }

            if (index > 0) {
                val params = item.root.layoutParams as ViewGroup.MarginLayoutParams
                params.topMargin = resources.getDimensionPixelSize(R.dimen.home_quick_card_spacing)
                item.root.layoutParams = params
            }

            binding.serviceRequestContainer.addView(item.root)
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
