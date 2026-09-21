package com.arwe.newproject.ui.booking

import com.arwe.newproject.data.remote.dto.CustomerServiceRequestResponse

sealed class ConfirmBookingUiState {
    object Idle : ConfirmBookingUiState()
    object Loading : ConfirmBookingUiState()
    data class Success(val response: CustomerServiceRequestResponse) : ConfirmBookingUiState()

    /** No confirmed service_type_id exists for this category yet - see serviceTypeIdFor()'s doc.
     * The API is never called for this state. */
    object UnsupportedCategory : ConfirmBookingUiState()

    /** customer_address_id could not be resolved from the incoming Intent extras - shouldn't
     * happen via normal navigation, but the API must never be called without it. No message
     * string here on purpose - the Activity resolves it via getString(), same as
     * CustomerOtpRegistrationUiState.MissingContext, keeping the ViewModel free of any Context
     * resource lookups outside messageFor()'s already-tested fallback path. */
    object MissingAddress : ConfirmBookingUiState()

    data class Error(val message: String) : ConfirmBookingUiState()
}
