package com.arwe.newproject.ui.addresses

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.arwe.newproject.R
import com.arwe.newproject.data.remote.dto.CustomerAddressResponse
import com.arwe.newproject.databinding.ActivityMyAddressesBinding
import com.arwe.newproject.ui.booking.AddAddressActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Standalone customer address management - separate from the booking flow's
 * AddressSelectionActivity (which requires category/product/problem intent extras and is never
 * reused here). Add/Edit both reuse AddAddressActivity (create vs PATCH branch - see its own
 * doc); Delete/Set Default call the repository directly.
 */
class MyAddressesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyAddressesBinding
    private val viewModel: MyAddressesViewModel by viewModels { MyAddressesViewModelFactory(application) }
    private lateinit var adapter: MyAddressesAdapter

    private val addOrEditAddressLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.loadAddresses()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyAddressesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }

        adapter = MyAddressesAdapter(
            onEdit = { address -> addOrEditAddressLauncher.launch(AddAddressActivity.newEditIntent(this, address)) },
            onDelete = { address -> confirmDelete(address) },
            onSetDefault = { address -> viewModel.setDefault(address) }
        )
        binding.addressesRecyclerView.adapter = adapter

        binding.addNewAddressButton.setOnClickListener {
            addOrEditAddressLauncher.launch(Intent(this, AddAddressActivity::class.java))
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.actionState.collect { state -> renderAction(state) }
            }
        }

        viewModel.loadAddresses()
    }

    private fun render(state: MyAddressesUiState) {
        binding.addressesProgress.visibility = View.GONE
        binding.addressesRecyclerView.visibility = View.GONE
        binding.emptyState.visibility = View.GONE

        when (state) {
            is MyAddressesUiState.Loading -> {
                binding.addressesProgress.visibility = View.VISIBLE
            }

            is MyAddressesUiState.Empty -> {
                binding.emptyState.visibility = View.VISIBLE
            }

            is MyAddressesUiState.Loaded -> {
                binding.addressesRecyclerView.visibility = View.VISIBLE
                adapter.submitList(state.addresses)
            }

            is MyAddressesUiState.Error -> {
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun renderAction(state: MyAddressesActionState) {
        when (state) {
            is MyAddressesActionState.Idle -> Unit // a successful action's own loadAddresses() drives uiState/render() instead

            is MyAddressesActionState.InProgress -> {
                binding.addressesProgress.visibility = View.VISIBLE
                binding.addressesRecyclerView.visibility = View.GONE
                binding.emptyState.visibility = View.GONE
            }

            is MyAddressesActionState.Error -> {
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                render(viewModel.uiState.value) // restore the list that was showing before this action
            }
        }
    }

    private fun confirmDelete(address: CustomerAddressResponse) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.my_addresses_delete_confirm_title)
            .setPositiveButton(R.string.my_addresses_delete_confirm_positive) { _, _ -> viewModel.deleteAddress(address.id) }
            .setNegativeButton(R.string.my_addresses_delete_confirm_negative, null)
            .show()
    }
}
