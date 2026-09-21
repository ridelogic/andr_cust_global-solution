package com.arwe.newproject.data.repository

import com.arwe.newproject.data.remote.dto.CustomerDto

sealed class RegisterResult {

    data class Success(val token: String, val customer: CustomerDto) : RegisterResult()

    /**
     * [message] is the backend's own "message" field - either Laravel's standard validation-error
     * envelope or the RegisterCustomerAction business-rule 422 (a duplicate mobile/email already
     * registered) - see AuthController::register()/RegisterCustomerAction::handle()/
     * bootstrap/app.php. [fieldErrors] is Laravel's "errors" map, present only for a standard
     * validation failure (the business-rule 422 has no "errors" key).
     */
    data class Error(
        val reason: ApiErrorReason,
        val message: String?,
        val fieldErrors: Map<String, List<String>>? = null
    ) : RegisterResult()
}
