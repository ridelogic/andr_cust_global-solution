package com.arwe.newproject.data.repository

import com.arwe.newproject.data.remote.dto.CustomerAddressResponse

sealed class AddressListResult {
    data class Success(val addresses: List<CustomerAddressResponse>) : AddressListResult()
    data class Error(
        val reason: ApiErrorReason,
        val message: String?,
        val fieldErrors: Map<String, List<String>>? = null
    ) : AddressListResult()
}

sealed class AddressResult {
    data class Success(val address: CustomerAddressResponse) : AddressResult()
    data class Error(
        val reason: ApiErrorReason,
        val message: String?,
        val fieldErrors: Map<String, List<String>>? = null
    ) : AddressResult()
}
