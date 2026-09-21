package com.arwe.newproject.ui.booking

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.arwe.newproject.R
import com.arwe.newproject.databinding.ActivityConfirmBookingBinding
import com.arwe.newproject.databinding.ItemSummaryBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ConfirmBookingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfirmBookingBinding
    private val viewModel: ConfirmBookingViewModel by viewModels { ConfirmBookingViewModelFactory(application) }

    private lateinit var category: ServiceCategory
    private lateinit var problem: Problem
    private var customerAddressId: Long? = null
    private var preferredDate: String? = null
    private var preferredTimeFrom: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConfirmBookingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        category = resolveCategory(intent.getStringExtra(ServiceCategoryActivity.EXTRA_SELECTED_CATEGORY))
        val product = resolveProduct(category, intent.getStringExtra(ProductSelectionActivity.EXTRA_SELECTED_PRODUCT))
        problem = resolveProblem(category, product, intent.getStringExtra(ProblemSelectionActivity.EXTRA_SELECTED_PROBLEM))
        val addressLabel = intent.getStringExtra(AddressSelectionActivity.EXTRA_SELECTED_ADDRESS_LABEL).orEmpty()
        val addressDetails = intent.getStringExtra(AddressSelectionActivity.EXTRA_SELECTED_ADDRESS_DETAILS).orEmpty()
        val dateIso = intent.getStringExtra(DateTimeSelectionActivity.EXTRA_SELECTED_DATE).orEmpty()
        val timeValue = intent.getStringExtra(DateTimeSelectionActivity.EXTRA_SELECTED_TIME).orEmpty()

        customerAddressId = intent.getStringExtra(AddressSelectionActivity.EXTRA_SELECTED_ADDRESS)?.toLongOrNull()
        preferredDate = dateIso.takeIf { it.isNotEmpty() }
        preferredTimeFrom = timeValue.takeIf { it.isNotEmpty() }

        binding.backButton.setOnClickListener { finish() }

        bindSummary(binding.summaryCategory, category.iconRes, R.string.confirm_booking_section_category, getString(category.titleRes))
        bindSummary(binding.summaryProduct, product.iconRes, R.string.confirm_booking_section_product, getString(product.titleRes))
        bindSummary(binding.summaryProblem, problem.iconRes, R.string.confirm_booking_section_problem, getString(problem.titleRes))
        bindSummary(
            binding.summaryAddress,
            R.drawable.ic_location,
            R.string.confirm_booking_section_address,
            "$addressLabel - $addressDetails"
        )
        bindSummary(binding.summaryDate, R.drawable.ic_calendar, R.string.confirm_booking_section_date, formatDate(dateIso))
        bindSummary(binding.summaryTime, R.drawable.ic_active_service, R.string.confirm_booking_section_time, formatTime(timeValue))

        binding.confirmBookingButton.setOnClickListener {
            viewModel.confirmBooking(
                category = category,
                problem = problem,
                customerAddressId = customerAddressId,
                preferredDate = preferredDate,
                preferredTimeFrom = preferredTimeFrom
            )
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: ConfirmBookingUiState) {
        when (state) {
            is ConfirmBookingUiState.Idle -> {
                binding.confirmBookingProgress.visibility = View.GONE
                binding.confirmBookingButton.isEnabled = true
            }

            is ConfirmBookingUiState.Loading -> {
                binding.confirmBookingProgress.visibility = View.VISIBLE
                binding.confirmBookingButton.isEnabled = false
            }

            is ConfirmBookingUiState.Success -> {
                binding.confirmBookingProgress.visibility = View.GONE
                binding.confirmBookingButton.isEnabled = false
                Snackbar.make(binding.root, R.string.confirm_booking_success, Snackbar.LENGTH_LONG).show()
            }

            is ConfirmBookingUiState.UnsupportedCategory -> {
                binding.confirmBookingProgress.visibility = View.GONE
                binding.confirmBookingButton.isEnabled = true
                Snackbar.make(binding.root, R.string.confirm_booking_error_unsupported_category, Snackbar.LENGTH_LONG).show()
            }

            is ConfirmBookingUiState.MissingAddress -> {
                binding.confirmBookingProgress.visibility = View.GONE
                binding.confirmBookingButton.isEnabled = true
                Snackbar.make(binding.root, R.string.confirm_booking_error_missing_address, Snackbar.LENGTH_LONG).show()
            }

            is ConfirmBookingUiState.Error -> {
                binding.confirmBookingProgress.visibility = View.GONE
                binding.confirmBookingButton.isEnabled = true
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun bindSummary(item: ItemSummaryBinding, iconRes: Int, labelRes: Int, value: String) {
        item.summaryIcon.setImageResource(iconRes)
        item.summaryLabel.setText(labelRes)
        item.summaryValue.text = value
    }

    private fun resolveCategory(id: String?): ServiceCategory =
        ServiceCategory.values().firstOrNull { it.id == id } ?: ServiceCategory.OTHER

    private fun resolveProduct(category: ServiceCategory, id: String?): Product {
        val products = ProductCatalog.productsFor(category)
        return products.firstOrNull { it.id == id } ?: products.first()
    }

    private fun resolveProblem(category: ServiceCategory, product: Product, id: String?): Problem {
        val problems = ProblemCatalog.problemsFor(category, product)
        return problems.firstOrNull { it.id == id } ?: problems.first()
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
