package com.arwe.newproject.ui.booking

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.arwe.newproject.R
import com.arwe.newproject.data.remote.dto.CustomerAddressResponse
import com.arwe.newproject.databinding.ActivityAddressSelectionBinding
import com.arwe.newproject.databinding.ItemAddressBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AddressSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddressSelectionBinding
    private lateinit var category: ServiceCategory
    private lateinit var product: Product
    private lateinit var problem: Problem

    private val viewModel: AddressSelectionViewModel by viewModels { AddressSelectionViewModelFactory(application) }

    private var selectedAddress: CustomerAddressResponse? = null
    private var pendingAutoSelectId: String? = null
    private val addressItems = mutableMapOf<Long, ItemAddressBinding>()

    private val addAddressLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val createdId = result.data?.getStringExtra(AddAddressActivity.EXTRA_CREATED_ADDRESS_ID)
        if (result.resultCode == RESULT_OK && createdId != null) {
            pendingAutoSelectId = createdId
            viewModel.loadAddresses()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddressSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        category = resolveCategory(intent.getStringExtra(ServiceCategoryActivity.EXTRA_SELECTED_CATEGORY))
        product = resolveProduct(category, intent.getStringExtra(ProductSelectionActivity.EXTRA_SELECTED_PRODUCT))
        problem = resolveProblem(category, product, intent.getStringExtra(ProblemSelectionActivity.EXTRA_SELECTED_PROBLEM))

        binding.backButton.setOnClickListener { finish() }

        binding.addAddressButton.setOnClickListener {
            addAddressLauncher.launch(Intent(this, AddAddressActivity::class.java))
        }

        binding.continueButton.setOnClickListener {
            val address = selectedAddress ?: return@setOnClickListener
            val intent = Intent(this, DateTimeSelectionActivity::class.java)
                .putExtra(ServiceCategoryActivity.EXTRA_SELECTED_CATEGORY, category.id)
                .putExtra(ProductSelectionActivity.EXTRA_SELECTED_PRODUCT, product.id)
                .putExtra(ProblemSelectionActivity.EXTRA_SELECTED_PROBLEM, problem.id)
                .putExtra(EXTRA_SELECTED_ADDRESS, address.id.toString())
                .putExtra(EXTRA_SELECTED_ADDRESS_LABEL, address.addressType)
                .putExtra(EXTRA_SELECTED_ADDRESS_DETAILS, formatAddressDetails(address))
            startActivity(intent)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: AddressListUiState) {
        binding.addressListProgress.visibility = View.GONE
        binding.addressEmptyState.visibility = View.GONE
        binding.addressContainer.visibility = View.GONE

        when (state) {
            is AddressListUiState.Loading -> {
                binding.addressListProgress.visibility = View.VISIBLE
            }

            is AddressListUiState.Empty -> {
                binding.addressEmptyState.visibility = View.VISIBLE
                selectedAddress = null
                binding.continueButton.isEnabled = false
            }

            is AddressListUiState.Loaded -> {
                binding.addressContainer.visibility = View.VISIBLE
                bindAddressList(state.addresses)
            }

            is AddressListUiState.Error -> {
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun bindAddressList(addresses: List<CustomerAddressResponse>) {
        binding.addressContainer.removeAllViews()
        addressItems.clear()

        val autoSelectId = pendingAutoSelectId
        pendingAutoSelectId = null
        val previousSelectedId = selectedAddress?.id
        selectedAddress = null

        addresses.forEachIndexed { index, address ->
            addAddressItem(address, index > 0)
        }

        val addressToSelect = addresses.firstOrNull { it.id.toString() == autoSelectId }
            ?: addresses.firstOrNull { it.id == previousSelectedId }
        if (addressToSelect != null) {
            selectAddress(addressToSelect)
        } else {
            binding.continueButton.isEnabled = false
        }
    }

    private fun addAddressItem(address: CustomerAddressResponse, addTopSpacing: Boolean) {
        val item = ItemAddressBinding.inflate(layoutInflater, binding.addressContainer, false)
        item.addressTitle.text = address.addressType
        item.addressDescription.text = formatAddressDetails(address)
        item.root.contentDescription = "${address.addressType}. ${formatAddressDetails(address)}"
        item.root.setOnClickListener { selectAddress(address) }

        if (addTopSpacing) {
            val params = item.root.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = resources.getDimensionPixelSize(R.dimen.home_quick_card_spacing)
            item.root.layoutParams = params
        }

        binding.addressContainer.addView(item.root)
        addressItems[address.id] = item
    }

    private fun selectAddress(address: CustomerAddressResponse) {
        selectedAddress = address
        addressItems.forEach { (id, item) -> item.root.isSelected = id == address.id }
        binding.continueButton.isEnabled = true
    }

    private fun formatAddressDetails(address: CustomerAddressResponse): String {
        val line = if (address.addressLine2.isNullOrBlank()) {
            address.addressLine1
        } else {
            "${address.addressLine1}, ${address.addressLine2}"
        }
        return getString(R.string.address_line_format, line, address.city, address.postalCode)
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

    companion object {
        const val EXTRA_SELECTED_ADDRESS = "extra_selected_address"
        const val EXTRA_SELECTED_ADDRESS_LABEL = "extra_selected_address_label"
        const val EXTRA_SELECTED_ADDRESS_DETAILS = "extra_selected_address_details"
    }
}
