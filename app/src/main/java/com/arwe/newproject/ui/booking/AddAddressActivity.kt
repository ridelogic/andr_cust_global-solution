package com.arwe.newproject.ui.booking

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.arwe.newproject.R
import com.arwe.newproject.data.remote.dto.CustomerAddressResponse
import com.arwe.newproject.databinding.ActivityAddAddressBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Handles both creating a new address (no edit extras - used standalone from MyAddressesActivity
 * or via newIntent() from the booking flow's AddressSelectionActivity) and editing an existing one
 * (via newEditIntent() - MyAddressesActivity only). Same form, same repository calls
 * (createAddress()/updateAddress()) - no second address form, no duplicated API logic.
 */
class AddAddressActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddAddressBinding
    private val viewModel: AddAddressViewModel by viewModels { AddAddressViewModelFactory(application) }
    private var editAddressId: Long? = null
    private var editIsDefault: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        editAddressId = intent.getLongExtra(EXTRA_EDIT_ADDRESS_ID, -1L).takeIf { it > 0 }
        editIsDefault = intent.getBooleanExtra(EXTRA_EDIT_IS_DEFAULT, false)
        if (editAddressId != null) {
            binding.headerTitle.text = getString(R.string.edit_address_title)
            binding.addressTypeEditText.setText(intent.getStringExtra(EXTRA_EDIT_ADDRESS_TYPE))
            binding.addressLine1EditText.setText(intent.getStringExtra(EXTRA_EDIT_ADDRESS_LINE1))
            binding.addressLine2EditText.setText(intent.getStringExtra(EXTRA_EDIT_ADDRESS_LINE2))
            binding.landmarkEditText.setText(intent.getStringExtra(EXTRA_EDIT_LANDMARK))
            binding.cityEditText.setText(intent.getStringExtra(EXTRA_EDIT_CITY))
            binding.stateEditText.setText(intent.getStringExtra(EXTRA_EDIT_STATE))
            binding.postalCodeEditText.setText(intent.getStringExtra(EXTRA_EDIT_POSTAL_CODE))
        }

        binding.backButton.setOnClickListener { finish() }

        binding.saveButton.setOnClickListener {
            val addressType = binding.addressTypeEditText.text?.toString().orEmpty()
            val addressLine1 = binding.addressLine1EditText.text?.toString().orEmpty()
            val addressLine2 = binding.addressLine2EditText.text?.toString().orEmpty()
            val landmark = binding.landmarkEditText.text?.toString().orEmpty()
            val city = binding.cityEditText.text?.toString().orEmpty()
            val state = binding.stateEditText.text?.toString().orEmpty()
            val postalCode = binding.postalCodeEditText.text?.toString().orEmpty()

            val result = viewModel.validate(addressType, addressLine1, city, state, postalCode)
            if (result.isValid) {
                val id = editAddressId
                if (id != null) {
                    viewModel.update(id, addressType, addressLine1, addressLine2, landmark, city, state, postalCode, editIsDefault)
                } else {
                    viewModel.save(addressType, addressLine1, addressLine2, landmark, city, state, postalCode)
                }
            }
        }

        viewModel.validationState.observe(this) { state ->
            binding.addressTypeInput.error =
                state.addressTypeError?.let { fieldErrorMessage(it, R.string.add_address_error_type_required) }
            binding.addressLine1Input.error =
                state.addressLine1Error?.let { fieldErrorMessage(it, R.string.add_address_error_line1_required) }
            binding.cityInput.error =
                state.cityError?.let { fieldErrorMessage(it, R.string.add_address_error_city_required) }
            binding.stateInput.error =
                state.stateError?.let { fieldErrorMessage(it, R.string.add_address_error_state_required) }
            binding.postalCodeInput.error =
                state.postalCodeError?.let { fieldErrorMessage(it, R.string.add_address_error_postal_code_required) }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun fieldErrorMessage(error: AddAddressFieldError, requiredMessageRes: Int): String = when (error) {
        AddAddressFieldError.REQUIRED -> getString(requiredMessageRes)
        AddAddressFieldError.TOO_LONG -> getString(R.string.add_address_error_too_long)
    }

    private fun render(state: AddAddressUiState) {
        when (state) {
            is AddAddressUiState.Idle -> {
                binding.saveProgress.visibility = View.GONE
                binding.saveButton.isEnabled = true
            }

            is AddAddressUiState.Loading -> {
                binding.saveProgress.visibility = View.VISIBLE
                binding.saveButton.isEnabled = false
            }

            is AddAddressUiState.Success -> {
                binding.saveProgress.visibility = View.GONE
                binding.saveButton.isEnabled = true
                val resultIntent = Intent().putExtra(EXTRA_CREATED_ADDRESS_ID, state.address.id.toString())
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }

            is AddAddressUiState.Error -> {
                binding.saveProgress.visibility = View.GONE
                binding.saveButton.isEnabled = true
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val EXTRA_CREATED_ADDRESS_ID = "extra_created_address_id"

        private const val EXTRA_EDIT_ADDRESS_ID = "extra_edit_address_id"
        private const val EXTRA_EDIT_ADDRESS_TYPE = "extra_edit_address_type"
        private const val EXTRA_EDIT_ADDRESS_LINE1 = "extra_edit_address_line1"
        private const val EXTRA_EDIT_ADDRESS_LINE2 = "extra_edit_address_line2"
        private const val EXTRA_EDIT_LANDMARK = "extra_edit_landmark"
        private const val EXTRA_EDIT_CITY = "extra_edit_city"
        private const val EXTRA_EDIT_STATE = "extra_edit_state"
        private const val EXTRA_EDIT_POSTAL_CODE = "extra_edit_postal_code"
        private const val EXTRA_EDIT_IS_DEFAULT = "extra_edit_is_default"

        /** Launches this screen pre-filled to edit [address] via PATCH instead of creating a new
         * one - see AddAddressViewModel.update(). */
        fun newEditIntent(context: Context, address: CustomerAddressResponse): Intent =
            Intent(context, AddAddressActivity::class.java)
                .putExtra(EXTRA_EDIT_ADDRESS_ID, address.id)
                .putExtra(EXTRA_EDIT_ADDRESS_TYPE, address.addressType)
                .putExtra(EXTRA_EDIT_ADDRESS_LINE1, address.addressLine1)
                .putExtra(EXTRA_EDIT_ADDRESS_LINE2, address.addressLine2)
                .putExtra(EXTRA_EDIT_LANDMARK, address.landmark)
                .putExtra(EXTRA_EDIT_CITY, address.city)
                .putExtra(EXTRA_EDIT_STATE, address.state)
                .putExtra(EXTRA_EDIT_POSTAL_CODE, address.postalCode)
                .putExtra(EXTRA_EDIT_IS_DEFAULT, address.isDefault)
    }
}
