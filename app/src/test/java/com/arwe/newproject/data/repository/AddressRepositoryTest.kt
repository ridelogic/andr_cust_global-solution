package com.arwe.newproject.data.repository

import com.arwe.newproject.data.remote.CustomerAuthApiService
import com.arwe.newproject.data.remote.dto.CustomerAddressRequest
import com.arwe.newproject.data.remote.dto.CustomerAddressResponse
import com.arwe.newproject.data.remote.dto.CustomerLoginRequest
import com.arwe.newproject.data.remote.dto.CustomerRegisterRequest
import com.arwe.newproject.data.remote.dto.CustomerRegisterResponse
import com.arwe.newproject.data.remote.dto.CustomerRegisterWithOtpRequest
import com.arwe.newproject.data.remote.dto.CustomerSendOtpRequest
import com.arwe.newproject.data.remote.dto.CustomerSendOtpResponse
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
        private val failure: Throwable? = null
    ) : CustomerAuthApiService {
        var capturedRequest: CustomerAddressRequest? = null

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
}
