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
import com.arwe.newproject.databinding.ActivityOtpVerificationBinding
import com.arwe.newproject.ui.home.HomeActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class OtpVerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOtpVerificationBinding
    private val viewModel: OtpVerificationViewModel by viewModels { OtpVerificationViewModelFactory(application) }
    private lateinit var mobile: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOtpVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mobile = intent.getStringExtra(EXTRA_MOBILE).orEmpty()
        // DEVELOPMENT-TESTING-ONLY AUTO-FILL: the real backend currently echoes the OTP back in
        // send-otp's response body only because this endpoint is still in its testing phase (see
        // AuthController::sendOtp()'s doc). This single line is the entire auto-fill behavior and
        // must be deleted - along with EXTRA_OTP below - the moment real SMS delivery replaces it;
        // nothing else in this screen depends on it. The OTP is never logged or persisted anywhere.
        val devOnlyAutoFillOtp = intent.getStringExtra(EXTRA_OTP).orEmpty()

        binding.mobileValueText.text = mobile
        binding.otpEditText.setText(devOnlyAutoFillOtp)

        binding.backButton.setOnClickListener { finish() }

        binding.verifyButton.setOnClickListener {
            val otp = binding.otpEditText.text?.toString().orEmpty()
            val result = viewModel.validate(otp)
            if (result.isValid) {
                viewModel.verifyOtp(mobile, otp)
            }
        }

        viewModel.validationState.observe(this) { state ->
            binding.otpInput.error = state.otpError?.let { getString(R.string.otp_verification_error_required) }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: OtpVerificationUiState) {
        when (state) {
            is OtpVerificationUiState.Idle -> {
                binding.verifyProgress.visibility = View.GONE
                binding.verifyButton.isEnabled = true
            }

            is OtpVerificationUiState.Loading -> {
                binding.verifyProgress.visibility = View.VISIBLE
                binding.verifyButton.isEnabled = false
            }

            is OtpVerificationUiState.ExistingCustomerSuccess -> {
                binding.verifyProgress.visibility = View.GONE
                binding.verifyButton.isEnabled = true
                val intent = Intent(this, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }

            is OtpVerificationUiState.RegistrationRequired -> {
                binding.verifyProgress.visibility = View.GONE
                binding.verifyButton.isEnabled = true
                val intent = Intent(this, CustomerOtpRegistrationActivity::class.java)
                    .putExtra(CustomerOtpRegistrationActivity.EXTRA_MOBILE, state.mobile)
                    .putExtra(CustomerOtpRegistrationActivity.EXTRA_REGISTRATION_TOKEN, state.registrationToken)
                startActivity(intent)
            }

            is OtpVerificationUiState.Error -> {
                binding.verifyProgress.visibility = View.GONE
                binding.verifyButton.isEnabled = true
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    companion object {
        const val EXTRA_MOBILE = "extra_mobile"
        const val EXTRA_OTP = "extra_otp"
    }
}
