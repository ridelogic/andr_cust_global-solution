package com.arwe.newproject.ui.booking

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.arwe.newproject.R
import com.arwe.newproject.databinding.ActivityAddAddressBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class AddAddressActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddAddressBinding
    private val viewModel: AddAddressViewModel by viewModels { AddAddressViewModelFactory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddAddressBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
                viewModel.save(addressType, addressLine1, addressLine2, landmark, city, state, postalCode)
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
    }
}
