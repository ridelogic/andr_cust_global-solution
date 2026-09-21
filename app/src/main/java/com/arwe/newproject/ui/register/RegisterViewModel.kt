package com.arwe.newproject.ui.register

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

enum class RegisterFieldError {
    REQUIRED, INVALID_FORMAT, TOO_LONG, TOO_SHORT, MISMATCH
}

data class RegisterValidationState(
    val fullNameError: RegisterFieldError? = null,
    val mobileError: RegisterFieldError? = null,
    val alternateMobileError: RegisterFieldError? = null,
    val emailError: RegisterFieldError? = null,
    val passwordError: RegisterFieldError? = null,
    val confirmPasswordError: RegisterFieldError? = null
) {
    val isValid: Boolean
        get() = fullNameError == null && mobileError == null && alternateMobileError == null &&
            emailError == null && passwordError == null && confirmPasswordError == null
}

class RegisterViewModel(
    application: Application,
    private val repository: CustomerAuthRepository,
    private val sessionManager: SessionManager
) : AndroidViewModel(application) {

    private val _validationState = MutableLiveData(RegisterValidationState())
    val validationState: LiveData<RegisterValidationState> = _validationState

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun validate(
        fullName: String,
        mobile: String,
        alternateMobile: String,
        email: String,
        password: String,
        confirmPassword: String
    ): RegisterValidationState {
        val trimmedFullName = fullName.trim()
        val trimmedMobile = mobile.trim()
        val trimmedAlternateMobile = alternateMobile.trim()
        val trimmedEmail = email.trim()

        val fullNameError = when {
            trimmedFullName.isEmpty() -> RegisterFieldError.REQUIRED
            trimmedFullName.length > MAX_NAME_LENGTH -> RegisterFieldError.TOO_LONG
            else -> null
        }

        val mobileError = when {
            trimmedMobile.isEmpty() -> RegisterFieldError.REQUIRED
            trimmedMobile.length > MAX_MOBILE_LENGTH -> RegisterFieldError.TOO_LONG
            !PHONE_PATTERN.matches(trimmedMobile) -> RegisterFieldError.INVALID_FORMAT
            else -> null
        }

        val alternateMobileError = when {
            trimmedAlternateMobile.isEmpty() -> null
            trimmedAlternateMobile.length > MAX_MOBILE_LENGTH -> RegisterFieldError.TOO_LONG
            !PHONE_PATTERN.matches(trimmedAlternateMobile) -> RegisterFieldError.INVALID_FORMAT
            else -> null
        }

        // Email is required by the real backend contract (AuthController::register()'s
        // 'email' => ['required', ...] rule) - not optional, despite the earlier local-only guess.
        val emailError = when {
            trimmedEmail.isEmpty() -> RegisterFieldError.REQUIRED
            trimmedEmail.length > MAX_EMAIL_LENGTH -> RegisterFieldError.TOO_LONG
            !EMAIL_PATTERN.matches(trimmedEmail) -> RegisterFieldError.INVALID_FORMAT
            else -> null
        }

        val passwordError = when {
            password.isEmpty() -> RegisterFieldError.REQUIRED
            password.length < MIN_PASSWORD_LENGTH -> RegisterFieldError.TOO_SHORT
            else -> null
        }

        val confirmPasswordError = when {
            confirmPassword.isEmpty() -> RegisterFieldError.REQUIRED
            confirmPassword != password -> RegisterFieldError.MISMATCH
            else -> null
        }

        val state = RegisterValidationState(
            fullNameError = fullNameError,
            mobileError = mobileError,
            alternateMobileError = alternateMobileError,
            emailError = emailError,
            passwordError = passwordError,
            confirmPasswordError = confirmPasswordError
        )
        _validationState.value = state
        return state
    }

    /**
     * Only called after [validate] has confirmed every field is valid. alternate_mobile is never
     * sent - the real POST customer/register endpoint does not accept it (see
     * CustomerAuthRepository's doc).
     */
    fun register(fullName: String, mobile: String, email: String, password: String, confirmPassword: String) {
        if (_uiState.value == RegisterUiState.Loading) return

        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            when (
                val result = repository.register(
                    name = fullName.trim(),
                    mobile = mobile.trim(),
                    email = email.trim(),
                    password = password,
                    passwordConfirmation = confirmPassword
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
                    _uiState.value = RegisterUiState.Success
                }

                is RegisterResult.Error -> {
                    _uiState.value = RegisterUiState.Error(messageFor(result))
                }
            }
        }
    }

    private fun messageFor(error: RegisterResult.Error): String {
        val app = getApplication<Application>()
        val fieldMessage = error.fieldErrors?.values?.firstOrNull()?.firstOrNull()
        return fieldMessage ?: error.message ?: app.getString(fallbackMessageResFor(error.reason))
    }

    companion object {
        private const val MAX_NAME_LENGTH = 255
        private const val MAX_MOBILE_LENGTH = 20
        private const val MAX_EMAIL_LENGTH = 255

        // Matches Password::min(8) from AuthController::register() exactly - Laravel's default
        // Password rule with no ->mixedCase()/->numbers()/->symbols() chained, so length only.
        private const val MIN_PASSWORD_LENGTH = 8

        // Mirrors the backend's own mobile regex exactly:
        // 'mobile' => [..., 'regex:/^[0-9+\-\s()]{7,20}$/'] in AuthController::register().
        private val PHONE_PATTERN = Regex("^[0-9+\\-\\s()]{7,20}$")

        private val EMAIL_PATTERN = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}

/**
 * The fixed fallback string resource for each [ApiErrorReason], used only when the backend gave no
 * usable message/field error of its own. Context-free on purpose so the mapping itself is directly
 * unit-testable. UNAUTHORIZED/FORBIDDEN/NOT_FOUND are not expected from this public endpoint under
 * normal use, but are handled defensively per the task's error-handling requirements.
 */
internal fun fallbackMessageResFor(reason: ApiErrorReason): Int = when (reason) {
    ApiErrorReason.VALIDATION_FAILED -> R.string.register_error_validation_generic
    ApiErrorReason.TOO_MANY_ATTEMPTS -> R.string.register_error_too_many_attempts
    ApiErrorReason.SERVER_ERROR -> R.string.register_error_server
    ApiErrorReason.NETWORK_UNAVAILABLE -> R.string.register_error_network_unavailable
    ApiErrorReason.TIMEOUT -> R.string.register_error_timeout
    ApiErrorReason.UNAUTHORIZED,
    ApiErrorReason.FORBIDDEN,
    ApiErrorReason.NOT_FOUND,
    ApiErrorReason.UNKNOWN -> R.string.register_error_generic
}
