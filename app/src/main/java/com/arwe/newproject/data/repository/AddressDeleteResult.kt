package com.arwe.newproject.data.repository

sealed class AddressDeleteResult {
    object Success : AddressDeleteResult()
    data class Error(
        val reason: ApiErrorReason,
        val message: String?,
        val fieldErrors: Map<String, List<String>>? = null
    ) : AddressDeleteResult()
}
