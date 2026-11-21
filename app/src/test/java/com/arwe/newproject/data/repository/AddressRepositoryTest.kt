package com.arwe.newproject.data.repository

import com.arwe.newproject.data.remote.CustomerAuthApiService
import com.arwe.newproject.data.remote.dto.CustomerAddressDeleteResponse
import com.arwe.newproject.data.remote.dto.CustomerAddressRequest
import com.arwe.newproject.data.remote.dto.CustomerAddressResponse
import com.arwe.newproject.data.remote.dto.CustomerLoginRequest
import com.arwe.newproject.data.remote.dto.CustomerRegisterRequest
import com.arwe.newproject.data.remote.dto.CustomerRegisterResponse
import com.arwe.newproject.data.remote.dto.CustomerRegisterWithOtpRequest
import com.arwe.newproject.data.remote.dto.CustomerSendOtpRequest
import com.arwe.newproject.data.remote.dto.CustomerSendOtpResponse
import com.arwe.newproject.data.remote.dto.CustomerServiceRequestDetailResponse
import com.arwe.newproject.data.remote.dto.CustomerServiceRequestListResponse
import com.arwe.newproject.data.remote.dto.CustomerServiceRequestRequest
import com.arwe.newproject.data.remote.dto.CustomerServiceRequestResponse
import com.arwe.newproject.data.remote.dto.CustomerVerifyOtpRequest
import com.arwe.newproject.data.remote.dto.CustomerVerifyOtpResponse
import com.arwe.newproject.data.remote.dto.ProductBatteryTypeResponse
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/**
 * Covers DefaultCustomerAuthRepository.createAddress() and getAddresses() - request construction
 * (including the always-false is_default), 200 parsing, 422 field-error mapping, and
 * network-failure mapping - using a fake CustomerAuthApiService so no real network call is ever
 * made.
 */
class AddressRepositoryTest {

    private class FakeApiService(
        private val createResponse: Response<CustomerAddressResponse>? = null,
        private val listResponse: Response<List<CustomerAddressResponse>>? = null,
        private val updateResponse: Response<CustomerAddressResponse>? = null,
        private val deleteResponse: Response<CustomerAddressDeleteResponse>? = null,
        private val failure: Throwable? = null
    ) : CustomerAuthApiService {
        var capturedRequest: CustomerAddressRequest? = null
        var capturedUpdateId: Long? = null
        var capturedUpdateRequest: CustomerAddressRequest? = null
        var capturedDeleteId: Long? = null

        override suspend fun register(request: CustomerRegisterRequest): Response<CustomerRegisterResponse> =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun login(request: CustomerLoginRequest): Response<CustomerRegisterResponse> =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun getAddresses(): Response<List<CustomerAddressResponse>> {
            failure?.let { throw it }
            return listResponse!!
        }

        override suspend fun createAddress(request: CustomerAddressRequest): Response<CustomerAddressResponse> {
            capturedRequest = request
            failure?.let { throw it }
            return createResponse!!
        }

        override suspend fun updateAddress(id: Long, request: CustomerAddressRequest): Response<CustomerAddressResponse> {
            capturedUpdateId = id
            capturedUpdateRequest = request
            failure?.let { throw it }
            return updateResponse!!
        }

        override suspend fun deleteAddress(id: Long): Response<CustomerAddressDeleteResponse> {
            capturedDeleteId = id
            failure?.let { throw it }
            return deleteResponse!!
        }

        override suspend fun sendOtp(request: CustomerSendOtpRequest): Response<CustomerSendOtpResponse> =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun verifyOtp(request: CustomerVerifyOtpRequest): Response<CustomerVerifyOtpResponse> =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun registerWithOtp(request: CustomerRegisterWithOtpRequest): Response<CustomerRegisterResponse> =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun getBatteryTypes(): Response<List<ProductBatteryTypeResponse>> =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun createServiceRequest(
            request: CustomerServiceRequestRequest
        ): Response<CustomerServiceRequestResponse> =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun getServiceRequests(): Response<CustomerServiceRequestListResponse> =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun getServiceRequestDetail(id: Long): Response<CustomerServiceRequestDetailResponse> =
            throw UnsupportedOperationException("not used by this test")
    }

    private fun address(id: Long = 42) = CustomerAddressResponse(
        id = id,
        addressType = "Home",
        addressLine1 = "221B Baker Street",
        addressLine2 = "Near Central Park",
        landmark = "Opposite Bus Stand",
        city = "Coimbatore",
        state = "Tamil Nadu",
        postalCode = "641035",
        latitude = null,
        longitude = null,
        isDefault = false
    )

    @Test
    fun `createAddress sends is_default false and the exact entered fields`() = runBlocking {
        val fake = FakeApiService(createResponse = Response.success(address()))
        val repository = DefaultCustomerAuthRepository(fake)

        repository.createAddress(
            addressType = "Home",
            addressLine1 = "221B Baker Street",
            addressLine2 = "Near Central Park",
            landmark = "Opposite Bus Stand",
            city = "Coimbatore",
            state = "Tamil Nadu",
            postalCode = "641035"
        )

        assertEquals(
            CustomerAddressRequest(
                addressType = "Home",
                addressLine1 = "221B Baker Street",
                addressLine2 = "Near Central Park",
                landmark = "Opposite Bus Stand",
                city = "Coimbatore",
                state = "Tamil Nadu",
                postalCode = "641035",
                isDefault = false
            ),
            fake.capturedRequest
        )
    }

    @Test
    fun `createAddress with optional fields omitted sends null for address_line2 and landmark`() = runBlocking {
        val fake = FakeApiService(createResponse = Response.success(address()))
        val repository = DefaultCustomerAuthRepository(fake)

        repository.createAddress(
            addressType = "Office",
            addressLine1 = "12 MG Road",
            addressLine2 = null,
            landmark = null,
            city = "Chennai",
            state = "Tamil Nadu",
            postalCode = "600001"
        )

        assertNull(fake.capturedRequest?.addressLine2)
        assertNull(fake.capturedRequest?.landmark)
    }

    @Test
    fun `successful createAddress 200 response is parsed into AddressResult Success with the real id`() = runBlocking {
        val fake = FakeApiService(createResponse = Response.success(address(id = 42)))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.createAddress("Home", "221B Baker Street", null, null, "Coimbatore", "Tamil Nadu", "641035")

        assertTrue(result is AddressResult.Success)
        assertEquals(42L, (result as AddressResult.Success).address.id)
    }

    @Test
    fun `HTTP 422 surfaces the backend field error, e g a missing address_line1`() = runBlocking {
        val errorBody = """
            {"message":"The address line1 field is required.","errors":{"address_line1":["The address line1 field is required."]}}
        """.trimIndent().toResponseBody("application/json".toMediaType())
        val fake = FakeApiService(createResponse = Response.error(422, errorBody))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.createAddress("Home", "", null, null, "Coimbatore", "Tamil Nadu", "641035")

        assertTrue(result is AddressResult.Error)
        val error = result as AddressResult.Error
        assertEquals(ApiErrorReason.VALIDATION_FAILED, error.reason)
        assertEquals(listOf("The address line1 field is required."), error.fieldErrors?.get("address_line1"))
    }

    @Test
    fun `network failure on createAddress maps to NETWORK_UNAVAILABLE`() = runBlocking {
        val fake = FakeApiService(failure = IOException("no connection"))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.createAddress("Home", "221B Baker Street", null, null, "Coimbatore", "Tamil Nadu", "641035")

        assertTrue(result is AddressResult.Error)
        assertEquals(ApiErrorReason.NETWORK_UNAVAILABLE, (result as AddressResult.Error).reason)
    }

    @Test
    fun `getAddresses parses the plain array response, including a newly created address`() = runBlocking {
        val fake = FakeApiService(listResponse = Response.success(listOf(address(id = 1), address(id = 42))))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.getAddresses()

        assertTrue(result is AddressListResult.Success)
        val ids = (result as AddressListResult.Success).addresses.map { it.id }
        assertEquals(listOf(1L, 42L), ids)
    }

    @Test
    fun `getAddresses HTTP 401 maps to UNAUTHORIZED`() = runBlocking {
        val errorBody = """{"message":"Unauthenticated."}""".toResponseBody("application/json".toMediaType())
        val fake = FakeApiService(listResponse = Response.error(401, errorBody))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.getAddresses()

        assertTrue(result is AddressListResult.Error)
        assertEquals(ApiErrorReason.UNAUTHORIZED, (result as AddressListResult.Error).reason)
    }

    @Test
    fun `network failure on getAddresses maps to NETWORK_UNAVAILABLE`() = runBlocking {
        val fake = FakeApiService(failure = IOException("no connection"))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.getAddresses()

        assertTrue(result is AddressListResult.Error)
        assertEquals(ApiErrorReason.NETWORK_UNAVAILABLE, (result as AddressListResult.Error).reason)
    }

    @Test
    fun `updateAddress sends the given id and full field set including the given is_default`() = runBlocking {
        val fake = FakeApiService(updateResponse = Response.success(address(id = 42).copy(isDefault = true)))
        val repository = DefaultCustomerAuthRepository(fake)

        repository.updateAddress(
            id = 42,
            addressType = "Office",
            addressLine1 = "12 MG Road",
            addressLine2 = null,
            landmark = null,
            city = "Chennai",
            state = "Tamil Nadu",
            postalCode = "600001",
            isDefault = true
        )

        assertEquals(42L, fake.capturedUpdateId)
        assertEquals(
            CustomerAddressRequest(
                addressType = "Office",
                addressLine1 = "12 MG Road",
                addressLine2 = null,
                landmark = null,
                city = "Chennai",
                state = "Tamil Nadu",
                postalCode = "600001",
                isDefault = true
            ),
            fake.capturedUpdateRequest
        )
    }

    @Test
    fun `successful updateAddress 200 response is parsed into AddressResult Success with the edited fields`() = runBlocking {
        val edited = address(id = 42).copy(addressLine1 = "New Line 1", isDefault = false)
        val fake = FakeApiService(updateResponse = Response.success(edited))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.updateAddress(42, "Home", "New Line 1", null, null, "Coimbatore", "Tamil Nadu", "641035", false)

        assertTrue(result is AddressResult.Success)
        assertEquals("New Line 1", (result as AddressResult.Success).address.addressLine1)
    }

    @Test
    fun `updateAddress HTTP 403 maps to FORBIDDEN, e g editing another customer's address`() = runBlocking {
        val errorBody = """{"message":"This action is unauthorized."}"""
            .toResponseBody("application/json".toMediaType())
        val fake = FakeApiService(updateResponse = Response.error(403, errorBody))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.updateAddress(99, "Home", "221B Baker Street", null, null, "Coimbatore", "Tamil Nadu", "641035", false)

        assertTrue(result is AddressResult.Error)
        assertEquals(ApiErrorReason.FORBIDDEN, (result as AddressResult.Error).reason)
    }

    @Test
    fun `network failure on updateAddress maps to NETWORK_UNAVAILABLE`() = runBlocking {
        val fake = FakeApiService(failure = IOException("no connection"))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.updateAddress(42, "Home", "221B Baker Street", null, null, "Coimbatore", "Tamil Nadu", "641035", false)

        assertTrue(result is AddressResult.Error)
        assertEquals(ApiErrorReason.NETWORK_UNAVAILABLE, (result as AddressResult.Error).reason)
    }

    @Test
    fun `deleteAddress sends the given id and a successful 200 response is parsed into AddressDeleteResult Success`() = runBlocking {
        val fake = FakeApiService(deleteResponse = Response.success(CustomerAddressDeleteResponse("Address removed.")))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.deleteAddress(42)

        assertEquals(42L, fake.capturedDeleteId)
        assertTrue(result is AddressDeleteResult.Success)
    }

    @Test
    fun `deleteAddress HTTP 404 maps to NOT_FOUND, e g an address that no longer exists`() = runBlocking {
        val errorBody = """{"message":"No query results for model."}"""
            .toResponseBody("application/json".toMediaType())
        val fake = FakeApiService(deleteResponse = Response.error(404, errorBody))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.deleteAddress(12345)

        assertTrue(result is AddressDeleteResult.Error)
        assertEquals(ApiErrorReason.NOT_FOUND, (result as AddressDeleteResult.Error).reason)
    }

    @Test
    fun `network failure on deleteAddress maps to NETWORK_UNAVAILABLE`() = runBlocking {
        val fake = FakeApiService(failure = IOException("no connection"))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.deleteAddress(42)

        assertTrue(result is AddressDeleteResult.Error)
        assertEquals(ApiErrorReason.NETWORK_UNAVAILABLE, (result as AddressDeleteResult.Error).reason)
    }
}
