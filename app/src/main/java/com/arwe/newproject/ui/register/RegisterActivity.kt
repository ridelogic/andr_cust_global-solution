package com.arwe.newproject.ui.register

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
import com.arwe.newproject.databinding.ActivityRegisterBinding
import com.arwe.newproject.ui.home.HomeActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels { RegisterViewModelFactory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }
        binding.loginLinkText.setOnClickListener { finish() }

        binding.registerButton.setOnClickListener {
            val fullName = binding.fullNameEditText.text?.toString().orEmpty()
            val mobile = binding.mobileEditText.text?.toString().orEmpty()
            val alternateMobile = binding.alternateMobileEditText.text?.toString().orEmpty()
            val email = binding.emailEditText.text?.toString().orEmpty()
            val password = binding.passwordEditText.text?.toString().orEmpty()
            val confirmPassword = binding.confirmPasswordEditText.text?.toString().orEmpty()

            val result = viewModel.validate(fullName, mobile, alternateMobile, email, password, confirmPassword)
            if (result.isValid) {
                viewModel.register(fullName, mobile, email, password, confirmPassword)
            }
        }

        viewModel.validationState.observe(this) { state ->
            binding.fullNameInput.error = state.fullNameError?.let { fullNameErrorMessage(it) }
            binding.mobileInput.error = state.mobileError?.let { mobileErrorMessage(it) }
            binding.alternateMobileInput.error = state.alternateMobileError?.let { alternateMobileErrorMessage(it) }
            binding.emailInput.error = state.emailError?.let { emailErrorMessage(it) }
            binding.passwordInput.error = state.passwordError?.let { passwordErrorMessage(it) }
            binding.confirmPasswordInput.error = state.confirmPasswordError?.let { confirmPasswordErrorMessage(it) }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    private fun render(state: RegisterUiState) {
        when (state) {
            is RegisterUiState.Idle -> {
                binding.registerProgress.visibility = View.GONE
                binding.registerButton.isEnabled = true
            }

            is RegisterUiState.Loading -> {
                binding.registerProgress.visibility = View.VISIBLE
                binding.registerButton.isEnabled = false
            }

            is RegisterUiState.Success -> {
                binding.registerProgress.visibility = View.GONE
                binding.registerButton.isEnabled = true
                Toast.makeText(this, R.string.register_success_message, Toast.LENGTH_SHORT).show()
                // Registration already returns an authenticated Sanctum token (saved by the
                // ViewModel) - go straight to Home, clearing Login/Register off the back stack.
                val intent = Intent(this, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }

            is RegisterUiState.Error -> {
                binding.registerProgress.visibility = View.GONE
                binding.registerButton.isEnabled = true
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun fullNameErrorMessage(error: RegisterFieldError): String = when (error) {
        RegisterFieldError.REQUIRED -> getString(R.string.register_error_full_name_required)
        RegisterFieldError.TOO_LONG -> getString(R.string.register_error_full_name_too_long)
        else -> getString(R.string.register_error_full_name_required)
    }

    private fun mobileErrorMessage(error: RegisterFieldError): String = when (error) {
        RegisterFieldError.REQUIRED -> getString(R.string.register_error_mobile_required)
        RegisterFieldError.INVALID_FORMAT -> getString(R.string.register_error_mobile_invalid)
        RegisterFieldError.TOO_LONG -> getString(R.string.register_error_mobile_too_long)
        else -> getString(R.string.register_error_mobile_required)
    }

    private fun alternateMobileErrorMessage(error: RegisterFieldError): String = when (error) {
        RegisterFieldError.INVALID_FORMAT -> getString(R.string.register_error_alternate_mobile_invalid)
        RegisterFieldError.TOO_LONG -> getString(R.string.register_error_alternate_mobile_too_long)
        else -> getString(R.string.register_error_alternate_mobile_invalid)
    }

    private fun emailErrorMessage(error: RegisterFieldError): String = when (error) {
        RegisterFieldError.REQUIRED -> getString(R.string.register_error_email_required)
        RegisterFieldError.INVALID_FORMAT -> getString(R.string.register_error_email_invalid)
        RegisterFieldError.TOO_LONG -> getString(R.string.register_error_email_too_long)
        else -> getString(R.string.register_error_email_required)
    }

    private fun passwordErrorMessage(error: RegisterFieldError): String = when (error) {
        RegisterFieldError.REQUIRED -> getString(R.string.register_error_password_required)
        RegisterFieldError.TOO_SHORT -> getString(R.string.register_error_password_too_short)
        else -> getString(R.string.register_error_password_required)
    }

    private fun confirmPasswordErrorMessage(error: RegisterFieldError): String = when (error) {
        RegisterFieldError.REQUIRED -> getString(R.string.register_error_confirm_password_required)
        RegisterFieldError.MISMATCH -> getString(R.string.register_error_confirm_password_mismatch)
        else -> getString(R.string.register_error_confirm_password_required)
    }
}
