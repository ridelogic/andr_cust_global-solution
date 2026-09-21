package com.arwe.newproject.ui.otp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arwe.newproject.R
import com.arwe.newproject.data.repository.ApiErrorReason
import com.arwe.newproject.data.repository.CustomerAuthRepository
import com.arwe.newproject.data.repository.VerifyOtpResult
import com.arwe.newproject.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class OtpFieldError { REQUIRED }

data class OtpValidationState(val otpError: OtpFieldError? = null) {
    val isValid: Boolean get() = otpError == null
}

class OtpVerificationViewModel(
    application: Application,
    private val repository: CustomerAuthRepository,
    private val sessionManager: SessionManager
) : AndroidViewModel(application) {

    private val _validationState = MutableLiveData(OtpValidationState())
    val validationState: LiveData<OtpValidationState> = _validationState

    private val _uiState = MutableStateFlow<OtpVerificationUiState>(OtpVerificationUiState.Idle)
    val uiState: StateFlow<OtpVerificationUiState> = _uiState.asStateFlow()

    fun validate(otp: String): OtpValidationState {
        val state = OtpValidationState(
            otpError = if (otp.trim().isEmpty()) OtpFieldError.REQUIRED else null
        )
        _validationState.value = state
        return state
    }

    /** Only called after [validate] has confirmed the OTP field is non-blank. */
    fun verifyOtp(mobile: String, otp: String) {
        if (_uiState.value == OtpVerificationUiState.Loading) return

        viewModelScope.launch {
            _uiState.value = OtpVerificationUiState.Loading
            when (val result = repository.verifyOtp(mobile, otp.trim())) {
                is VerifyOtpResult.ExistingCustomer -> {
                    // Token/session are only ever saved for this confirmed, verified branch -
                    // never for RegistrationRequired, and never before this point.
                    sessionManager.saveSession(
                        token = result.token,
                        customerCode = result.customer.customerCode,
                        name = result.customer.name,
                        mobile = result.customer.mobile,
                        email = result.customer.email
                    )
                    _uiState.value = OtpVerificationUiState.ExistingCustomerSuccess(result.token, result.customer)
                }

                is VerifyOtpResult.RegistrationRequired -> {
                    _uiState.value = OtpVerificationUiState.RegistrationRequired(result.mobile, result.registrationToken)
                }

                is VerifyOtpResult.Error -> {
                    _uiState.value = OtpVerificationUiState.Error(messageFor(result))
                }
            }
        }
    }

    private fun messageFor(error: VerifyOtpResult.Error): String {
        val app = getApplication<Application>()
        val fieldMessage = error.fieldErrors?.values?.firstOrNull()?.firstOrNull()
        return fieldMessage ?: error.message ?: app.getString(fallbackOtpVerificationMessageResFor(error.reason))
    }
}

/**
 * The fixed fallback string resource for each [ApiErrorReason], used only when the backend gave no
 * usable message/field error of its own (e.g. "Invalid OTP." / "OTP has expired..." normally
 * arrive as the top-level "message" on a 422 and are shown as-is, never replaced by this).
 */
internal fun fallbackOtpVerificationMessageResFor(reason: ApiErrorReason): Int = when (reason) {
    ApiErrorReason.VALIDATION_FAILED -> R.string.otp_verification_error_validation_generic
    ApiErrorReason.TOO_MANY_ATTEMPTS -> R.string.otp_verification_error_too_many_attempts
    ApiErrorReason.SERVER_ERROR -> R.string.otp_verification_error_server
    ApiErrorReason.NETWORK_UNAVAILABLE -> R.string.otp_verification_error_network_unavailable
    ApiErrorReason.TIMEOUT -> R.string.otp_verification_error_timeout
    ApiErrorReason.UNAUTHORIZED,
    ApiErrorReason.FORBIDDEN,
    ApiErrorReason.NOT_FOUND,
    ApiErrorReason.UNKNOWN -> R.string.otp_verification_error_generic
}
