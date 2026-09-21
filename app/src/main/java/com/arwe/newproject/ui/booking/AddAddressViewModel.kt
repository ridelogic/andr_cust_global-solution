package com.arwe.newproject.ui.booking

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.arwe.newproject.R
import com.arwe.newproject.data.repository.AddressResult
import com.arwe.newproject.data.repository.ApiErrorReason
import com.arwe.newproject.data.repository.CustomerAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AddAddressFieldError { REQUIRED, TOO_LONG }

data class AddAddressValidationState(
    val addressTypeError: AddAddressFieldError? = null,
    val addressLine1Error: AddAddressFieldError? = null,
    val cityError: AddAddressFieldError? = null,
    val stateError: AddAddressFieldError? = null,
    val postalCodeError: AddAddressFieldError? = null
) {
    val isValid: Boolean
        get() = addressTypeError == null && addressLine1Error == null &&
            cityError == null && stateError == null && postalCodeError == null
}

class AddAddressViewModel(
    application: Application,
    private val repository: CustomerAuthRepository
) : AndroidViewModel(application) {

    private val _validationState = MutableLiveData(AddAddressValidationState())
    val validationState: LiveData<AddAddressValidationState> = _validationState

    private val _uiState = MutableStateFlow<AddAddressUiState>(AddAddressUiState.Idle)
    val uiState: StateFlow<AddAddressUiState> = _uiState.asStateFlow()

    /**
     * Required fields mirror AddressController::validated() exactly: address_type,
     * address_line1, city, state, postal_code are all `required`; address_line2/landmark are
     * `nullable` and therefore not validated here beyond max length.
     */
    fun validate(
        addressType: String,
        addressLine1: String,
        city: String,
        state: String,
        postalCode: String
    ): AddAddressValidationState {
        val result = AddAddressValidationState(
            addressTypeError = requiredFieldError(addressType.trim(), MAX_TEXT_LENGTH),
            addressLine1Error = requiredFieldError(addressLine1.trim(), MAX_TEXT_LENGTH),
            cityError = requiredFieldError(city.trim(), MAX_TEXT_LENGTH),
            stateError = requiredFieldError(state.trim(), MAX_TEXT_LENGTH),
            postalCodeError = requiredFieldError(postalCode.trim(), MAX_POSTAL_CODE_LENGTH)
        )
        _validationState.value = result
        return result
    }

    private fun requiredFieldError(value: String, maxLength: Int): AddAddressFieldError? = when {
        value.isEmpty() -> AddAddressFieldError.REQUIRED
        value.length > maxLength -> AddAddressFieldError.TOO_LONG
        else -> null
    }

    /** Only called after [validate] has confirmed every required field is valid. */
    fun save(
        addressType: String,
        addressLine1: String,
        addressLine2: String,
        landmark: String,
        city: String,
        state: String,
        postalCode: String
    ) {
        if (_uiState.value == AddAddressUiState.Loading) return

        viewModelScope.launch {
            _uiState.value = AddAddressUiState.Loading
            when (
                val result = repository.createAddress(
                    addressType = addressType.trim(),
                    addressLine1 = addressLine1.trim(),
                    addressLine2 = addressLine2.trim().takeIf { it.isNotEmpty() },
                    landmark = landmark.trim().takeIf { it.isNotEmpty() },
                    city = city.trim(),
                    state = state.trim(),
                    postalCode = postalCode.trim()
                )
            ) {
                is AddressResult.Success -> _uiState.value = AddAddressUiState.Success(result.address)
                is AddressResult.Error -> _uiState.value = AddAddressUiState.Error(messageFor(result))
            }
        }
    }

    private fun messageFor(error: AddressResult.Error): String {
        val app = getApplication<Application>()
        val fieldMessage = error.fieldErrors?.values?.firstOrNull()?.firstOrNull()
        return fieldMessage ?: error.message ?: app.getString(fallbackAddAddressMessageResFor(error.reason))
    }

    companion object {
        private const val MAX_TEXT_LENGTH = 255
        private const val MAX_POSTAL_CODE_LENGTH = 20
    }
}

/**
 * Fallback string resource per [ApiErrorReason], used only when the backend gave no usable
 * message/field error of its own. Context-free and screen-specific on purpose (mirrors
 * RegisterViewModel/LoginViewModel's own equivalents) - never shared across screens, since the
 * right wording depends on which call actually failed.
 */
internal fun fallbackAddAddressMessageResFor(reason: ApiErrorReason): Int = when (reason) {
    ApiErrorReason.VALIDATION_FAILED -> R.string.add_address_error_validation_generic
    ApiErrorReason.TOO_MANY_ATTEMPTS -> R.string.add_address_error_too_many_attempts
    ApiErrorReason.SERVER_ERROR -> R.string.add_address_error_server
    ApiErrorReason.NETWORK_UNAVAILABLE -> R.string.add_address_error_network_unavailable
    ApiErrorReason.TIMEOUT -> R.string.add_address_error_timeout
    ApiErrorReason.UNAUTHORIZED -> R.string.add_address_error_unauthorized
    ApiErrorReason.FORBIDDEN,
    ApiErrorReason.NOT_FOUND,
    ApiErrorReason.UNKNOWN -> R.string.add_address_error_generic
}
