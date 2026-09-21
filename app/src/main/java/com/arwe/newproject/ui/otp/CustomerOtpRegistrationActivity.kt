package com.arwe.newproject.ui.otp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.arwe.newproject.R
import com.arwe.newproject.databinding.ActivityCustomerOtpRegistrationBinding
import com.arwe.newproject.ui.home.HomeActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Reached only when verify-otp returned requires_registration: true. The registration_token is
 * held only in memory (an Intent extra) for the lifetime of this screen - never written to
 * SharedPreferences, DataStore, or logs; it is no longer needed once registration succeeds.
 */
class CustomerOtpRegistrationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomerOtpRegistrationBinding
    private val viewModel: CustomerOtpRegistrationViewModel by viewModels {
        CustomerOtpRegistrationViewModelFactory(application)
    }
    private lateinit var mobile: String
    private lateinit var registrationToken: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerOtpRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mobile = intent.getStringExtra(EXTRA_MOBILE).orEmpty()
        registrationToken = intent.getStringExtra(EXTRA_REGISTRATION_TOKEN).orEmpty()

        binding.mobileValueText.text = mobile

        binding.backButton.setOnClickListener { finish() }

        binding.createAccountButton.setOnClickListener {
            val name = binding.nameEditText.text?.toString().orEmpty()
            val email = binding.emailEditText.text?.toString().orEmpty()
            val result = viewModel.validate(name, email)
            if (result.isValid) {
                viewModel.register(mobile, name, email, registrationToken)
            }
        }

        viewModel.validationState.observe(this) { state ->
            binding.nameInput.error = state.nameError?.let { nameErrorMessage(it) }
            binding.emailInput.error = state.emailError?.let { emailErrorMessage(it) }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: CustomerOtpRegistrationUiState) {
        when (state) {
            is CustomerOtpRegistrationUiState.Idle -> {
                binding.createAccountProgress.visibility = View.GONE
                binding.createAccountButton.isEnabled = true
            }

            is CustomerOtpRegistrationUiState.Loading -> {
                binding.createAccountProgress.visibility = View.VISIBLE
                binding.createAccountButton.isEnabled = false
            }

            is CustomerOtpRegistrationUiState.Success -> {
                binding.createAccountProgress.visibility = View.GONE
                binding.createAccountButton.isEnabled = true
                val intent = Intent(this, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }

            is CustomerOtpRegistrationUiState.MissingContext -> {
                binding.createAccountProgress.visibility = View.GONE
                binding.createAccountButton.isEnabled = true
                Snackbar.make(binding.root, R.string.otp_registration_error_missing_context, Snackbar.LENGTH_LONG).show()
            }

            is CustomerOtpRegistrationUiState.Error -> {
                binding.createAccountProgress.visibility = View.GONE
                binding.createAccountButton.isEnabled = true
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun nameErrorMessage(error: OtpRegistrationFieldError): String = when (error) {
        OtpRegistrationFieldError.REQUIRED -> getString(R.string.otp_registration_error_name_required)
        OtpRegistrationFieldError.TOO_LONG -> getString(R.string.otp_registration_error_name_too_long)
        OtpRegistrationFieldError.INVALID_FORMAT -> getString(R.string.otp_registration_error_name_required)
    }

    private fun emailErrorMessage(error: OtpRegistrationFieldError): String = when (error) {
        OtpRegistrationFieldError.INVALID_FORMAT -> getString(R.string.otp_registration_error_email_invalid)
        OtpRegistrationFieldError.TOO_LONG -> getString(R.string.otp_registration_error_email_too_long)
        OtpRegistrationFieldError.REQUIRED -> getString(R.string.otp_registration_error_email_invalid)
    }

    companion object {
        const val EXTRA_MOBILE = "extra_mobile"
        const val EXTRA_REGISTRATION_TOKEN = "extra_registration_token"
    }
}
