package com.arwe.newproject.data.repository

/**
 * Reasons an API call failed, derived only from transport-level facts (HTTP status code,
 * IOException/timeout) - never from guessed response body fields.
 */
enum class ApiErrorReason {
    UNAUTHORIZED,        // HTTP 401
    FORBIDDEN,            // HTTP 403
    NOT_FOUND,            // HTTP 404
    VALIDATION_FAILED,    // HTTP 422 - either Laravel's standard {message, errors} shape, or the
                           // RegisterCustomerAction business-rule {message} only (see bootstrap/app.php)
    TOO_MANY_ATTEMPTS,    // HTTP 429
    SERVER_ERROR,         // HTTP 5xx
    NETWORK_UNAVAILABLE,  // no connectivity / IOException
    TIMEOUT,              // socket timeout
    UNKNOWN
}
