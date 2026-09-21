package com.arwe.newproject.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arwe.newproject.R
import com.arwe.newproject.data.repository.ApiErrorReason
import com.arwe.newproject.data.repository.CustomerAuthRepository
import com.arwe.newproject.data.repository.SendOtpResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class LoginFieldError { REQUIRED, INVALID_FORMAT }

data class LoginValidationState(val mobileError: LoginFieldError? = null) {
    val isValid: Boolean get() = mobileError == null
}

class LoginViewModel(
    application: Application,
    private val repository: CustomerAuthRepository
) : AndroidViewModel(application) {

    private val _validationState = MutableLiveData(LoginValidationState())
    val validationState: LiveData<LoginValidationState> = _validationState

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * The backend's own send-otp validation only requires a non-empty string (see
     * AuthController::sendOtp()'s $request->validate([...])) - no format rule is enforced
     * server-side. Android still validates the shape here using the exact same phone regex
     * RegisterViewModel already uses, per "reuse the existing project's phone validation
     * convention" rather than inventing a new one.
     */
    fun validate(mobile: String): LoginValidationState {
        val trimmedMobile = mobile.trim()

        val mobileError = when {
            trimmedMobile.isEmpty() -> LoginFieldError.REQUIRED
            !PHONE_PATTERN.matches(trimmedMobile) -> LoginFieldError.INVALID_FORMAT
            else -> null
        }

        val state = LoginValidationState(mobileError)
        _validationState.value = state
        return state
    }

    /** Only called after [validate] has confirmed the mobile field is valid. */
    fun sendOtp(mobile: String) {
        if (_uiState.value == LoginUiState.Loading) return

        val trimmedMobile = mobile.trim()
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            when (val result = repository.sendOtp(trimmedMobile)) {
                is SendOtpResult.Success -> {
                    _uiState.value = LoginUiState.Success(trimmedMobile, result.response.otp)
                }

                is SendOtpResult.Error -> {
                    _uiState.value = LoginUiState.Error(messageFor(result))
                }
            }
        }
    }

    private fun messageFor(error: SendOtpResult.Error): String {
        val app = getApplication<Application>()
        val fieldMessage = error.fieldErrors?.values?.firstOrNull()?.firstOrNull()
        return fieldMessage ?: error.message ?: app.getString(fallbackMessageResFor(error.reason))
    }

    private companion object {
        // Mirrors RegisterViewModel's own PHONE_PATTERN exactly - same convention, not a new rule.
        val PHONE_PATTERN = Regex("^[0-9+\\-\\s()]{7,20}$")
    }
}

/**
 * The fixed fallback string resource for each [ApiErrorReason], used only when the backend gave no
 * usable message/field error of its own. Context-free and Login-specific on purpose (see
 * RegisterViewModel's equivalent for the same reasoning) - never reused across screens, since a
 * failure reason's right wording depends on which call actually failed.
 */
internal fun fallbackMessageResFor(reason: ApiErrorReason): Int = when (reason) {
    ApiErrorReason.VALIDATION_FAILED -> R.string.login_error_validation_generic
    ApiErrorReason.TOO_MANY_ATTEMPTS -> R.string.login_error_too_many_attempts
    ApiErrorReason.SERVER_ERROR -> R.string.login_error_server
    ApiErrorReason.NETWORK_UNAVAILABLE -> R.string.login_error_network_unavailable
    ApiErrorReason.TIMEOUT -> R.string.login_error_timeout
    ApiErrorReason.UNAUTHORIZED,
    ApiErrorReason.FORBIDDEN,
    ApiErrorReason.NOT_FOUND,
    ApiErrorReason.UNKNOWN -> R.string.login_error_generic
}
