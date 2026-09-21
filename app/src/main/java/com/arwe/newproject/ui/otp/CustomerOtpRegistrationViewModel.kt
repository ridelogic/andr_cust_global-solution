package com.arwe.newproject.ui.otp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arwe.newproject.R
import com.arwe.newproject.data.repository.ApiErrorReason
import com.arwe.newproject.data.repository.CustomerAuthRepository
import com.arwe.newproject.data.repository.RegisterResult
import com.arwe.newproject.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class OtpRegistrationFieldError { REQUIRED, INVALID_FORMAT, TOO_LONG }

data class OtpRegistrationValidationState(
    val nameError: OtpRegistrationFieldError? = null,
    val emailError: OtpRegistrationFieldError? = null
) {
    val isValid: Boolean get() = nameError == null && emailError == null
}

class CustomerOtpRegistrationViewModel(
    application: Application,
    private val repository: CustomerAuthRepository,
    private val sessionManager: SessionManager
) : AndroidViewModel(application) {

    private val _validationState = MutableLiveData(OtpRegistrationValidationState())
    val validationState: LiveData<OtpRegistrationValidationState> = _validationState

    private val _uiState = MutableStateFlow<CustomerOtpRegistrationUiState>(CustomerOtpRegistrationUiState.Idle)
    val uiState: StateFlow<CustomerOtpRegistrationUiState> = _uiState.asStateFlow()

    /**
     * Mirrors AuthController::registerWithOtp()'s own rules: name required (max 255, matching
     * 'name' => ['required','string','max:255']); email optional but validated when supplied,
     * using the exact same email regex RegisterViewModel/LoginViewModel already use - no new rule
     * invented.
     */
    fun validate(name: String, email: String): OtpRegistrationValidationState {
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()

        val nameError = when {
            trimmedName.isEmpty() -> OtpRegistrationFieldError.REQUIRED
            trimmedName.length > MAX_NAME_LENGTH -> OtpRegistrationFieldError.TOO_LONG
            else -> null
        }

        val emailError = when {
            trimmedEmail.isEmpty() -> null
            trimmedEmail.length > MAX_EMAIL_LENGTH -> OtpRegistrationFieldError.TOO_LONG
            !EMAIL_PATTERN.matches(trimmedEmail) -> OtpRegistrationFieldError.INVALID_FORMAT
            else -> null
        }

        val state = OtpRegistrationValidationState(nameError, emailError)
        _validationState.value = state
        return state
    }

    /**
     * Only called after [validate] has confirmed name/email are valid. mobile/registrationToken
     * come from the previous screen's Intent extras rather than user input, so they're checked
     * here as a precondition instead of via [validate]/a form field - if either is missing, the
     * API is never called.
     */
    fun register(mobile: String, name: String, email: String, registrationToken: String) {
        if (_uiState.value == CustomerOtpRegistrationUiState.Loading) return

        if (mobile.isBlank() || registrationToken.isBlank()) {
            _uiState.value = CustomerOtpRegistrationUiState.MissingContext
            return
        }

        viewModelScope.launch {
            _uiState.value = CustomerOtpRegistrationUiState.Loading
            val trimmedEmail = email.trim().takeIf { it.isNotEmpty() }
            when (
                val result = repository.registerWithOtp(
                    mobile = mobile,
                    name = name.trim(),
                    email = trimmedEmail,
                    registrationToken = registrationToken
                )
            ) {
                is RegisterResult.Success -> {
                    sessionManager.saveSession(
                        token = result.token,
                        customerCode = result.customer.customerCode,
                        name = result.customer.name,
                        mobile = result.customer.mobile,
                        email = result.customer.email
                    )
                    _uiState.value = CustomerOtpRegistrationUiState.Success(result.token, result.customer)
                }

                is RegisterResult.Error -> {
                    _uiState.value = CustomerOtpRegistrationUiState.Error(messageFor(result))
                }
            }
        }
    }

    private fun messageFor(error: RegisterResult.Error): String {
        val app = getApplication<Application>()
        val fieldMessage = error.fieldErrors?.values?.firstOrNull()?.firstOrNull()
        return fieldMessage ?: error.message ?: app.getString(fallbackOtpRegistrationMessageResFor(error.reason))
    }

    companion object {
        private const val MAX_NAME_LENGTH = 255
        private const val MAX_EMAIL_LENGTH = 255

        // Mirrors RegisterViewModel/LoginViewModel's own EMAIL_PATTERN exactly.
        private val EMAIL_PATTERN = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}

/**
 * The fixed fallback string resource for each [ApiErrorReason], used only when the backend gave no
 * usable message/field error of its own.
 */
internal fun fallbackOtpRegistrationMessageResFor(reason: ApiErrorReason): Int = when (reason) {
    ApiErrorReason.VALIDATION_FAILED -> R.string.otp_registration_error_validation_generic
    ApiErrorReason.TOO_MANY_ATTEMPTS -> R.string.otp_registration_error_too_many_attempts
    ApiErrorReason.SERVER_ERROR -> R.string.otp_registration_error_server
    ApiErrorReason.NETWORK_UNAVAILABLE -> R.string.otp_registration_error_network_unavailable
    ApiErrorReason.TIMEOUT -> R.string.otp_registration_error_timeout
    ApiErrorReason.UNAUTHORIZED,
    ApiErrorReason.FORBIDDEN,
    ApiErrorReason.NOT_FOUND,
    ApiErrorReason.UNKNOWN -> R.string.otp_registration_error_generic
}
