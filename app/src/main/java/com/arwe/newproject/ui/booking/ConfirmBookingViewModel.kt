package com.arwe.newproject.ui.booking

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arwe.newproject.R
import com.arwe.newproject.data.repository.ApiErrorReason
import com.arwe.newproject.data.repository.CustomerAuthRepository
import com.arwe.newproject.data.repository.ServiceRequestResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConfirmBookingViewModel(
    application: Application,
    private val repository: CustomerAuthRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ConfirmBookingUiState>(ConfirmBookingUiState.Idle)
    val uiState: StateFlow<ConfirmBookingUiState> = _uiState.asStateFlow()

    /**
     * preferredTimeTo is deliberately never sent: this screen captures one time point per
     * booking (see DateTimeSelectionActivity), and the backend's preferred_time_to represents the
     * end of a service window - deriving one from a single slot would require an appointment
     * duration rule that hasn't been decided anywhere, so it is left out entirely (both time
     * fields are nullable server-side, so omitting it is a valid request, not an invented value).
     */
    fun confirmBooking(
        category: ServiceCategory,
        problem: Problem,
        customerAddressId: Long?,
        preferredDate: String?,
        preferredTimeFrom: String?
    ) {
        if (_uiState.value == ConfirmBookingUiState.Loading) return

        val serviceTypeId = serviceTypeIdFor(category, problem)
        if (serviceTypeId == null) {
            _uiState.value = ConfirmBookingUiState.UnsupportedCategory
            return
        }

        if (customerAddressId == null) {
            _uiState.value = ConfirmBookingUiState.MissingAddress
            return
        }

        viewModelScope.launch {
            _uiState.value = ConfirmBookingUiState.Loading
            when (
                val result = repository.createServiceRequest(
                    serviceTypeId = serviceTypeId,
                    customerAddressId = customerAddressId,
                    complaint = problem.complaintValue,
                    preferredDate = preferredDate,
                    preferredTimeFrom = preferredTimeFrom,
                    preferredTimeTo = null
                )
            ) {
                is ServiceRequestResult.Success -> _uiState.value = ConfirmBookingUiState.Success(result.response)
                is ServiceRequestResult.Error -> _uiState.value = ConfirmBookingUiState.Error(messageFor(result))
            }
        }
    }

    private fun messageFor(error: ServiceRequestResult.Error): String {
        val app = getApplication<Application>()
        val fieldMessage = error.fieldErrors?.values?.firstOrNull()?.firstOrNull()
        return fieldMessage ?: error.message ?: app.getString(fallbackConfirmBookingMessageResFor(error.reason))
    }
}

internal fun fallbackConfirmBookingMessageResFor(reason: ApiErrorReason): Int = when (reason) {
    ApiErrorReason.VALIDATION_FAILED -> R.string.confirm_booking_error_validation_generic
    ApiErrorReason.TOO_MANY_ATTEMPTS -> R.string.confirm_booking_error_too_many_attempts
    ApiErrorReason.SERVER_ERROR -> R.string.confirm_booking_error_server
    ApiErrorReason.NETWORK_UNAVAILABLE -> R.string.confirm_booking_error_network_unavailable
    ApiErrorReason.TIMEOUT -> R.string.confirm_booking_error_timeout
    ApiErrorReason.UNAUTHORIZED -> R.string.confirm_booking_error_unauthorized
    ApiErrorReason.FORBIDDEN,
    ApiErrorReason.NOT_FOUND,
    ApiErrorReason.UNKNOWN -> R.string.confirm_booking_error_generic
}
