package com.arwe.newproject.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.arwe.newproject.R
import com.arwe.newproject.databinding.ActivityLoginBinding
import com.arwe.newproject.ui.otp.OtpVerificationActivity
import com.arwe.newproject.ui.register.RegisterActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels { LoginViewModelFactory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }

        binding.googleButton.setOnClickListener {
            // UI placeholder only; Google authentication is not implemented yet.
            Toast.makeText(this, R.string.login_google_placeholder, Toast.LENGTH_SHORT).show()
        }

        binding.registerText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.sendOtpButton.setOnClickListener {
            val mobile = binding.mobileEditText.text?.toString().orEmpty()
            val result = viewModel.validate(mobile)
            if (result.isValid) {
                viewModel.sendOtp(mobile)
            }
        }

        viewModel.validationState.observe(this) { state ->
            binding.mobileInput.error = state.mobileError?.let { mobileErrorMessage(it) }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: LoginUiState) {
        when (state) {
            is LoginUiState.Idle -> {
                binding.sendOtpProgress.visibility = View.GONE
                binding.sendOtpButton.isEnabled = true
            }

            is LoginUiState.Loading -> {
                binding.sendOtpProgress.visibility = View.VISIBLE
                binding.sendOtpButton.isEnabled = false
            }

            is LoginUiState.Success -> {
                binding.sendOtpProgress.visibility = View.GONE
                binding.sendOtpButton.isEnabled = true
                val intent = Intent(this, OtpVerificationActivity::class.java)
                    .putExtra(OtpVerificationActivity.EXTRA_MOBILE, state.mobile)
                    .putExtra(OtpVerificationActivity.EXTRA_OTP, state.otp)
                startActivity(intent)
            }

            is LoginUiState.Error -> {
                binding.sendOtpProgress.visibility = View.GONE
                binding.sendOtpButton.isEnabled = true
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun mobileErrorMessage(error: LoginFieldError): String = when (error) {
        LoginFieldError.REQUIRED -> getString(R.string.login_error_mobile_required)
        LoginFieldError.INVALID_FORMAT -> getString(R.string.login_error_mobile_invalid)
    }
}
