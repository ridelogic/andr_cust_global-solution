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
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/**
 * Covers DefaultCustomerAuthRepository.createServiceRequest() only - request construction, 201
 * parsing, 422 mapping, and network-failure mapping - using a fake CustomerAuthApiService so no
 * real network call is ever made.
 */
class ServiceRequestRepositoryTest {

    private class FakeApiService(
        private val response: Response<CustomerServiceRequestResponse>? = null,
        private val failure: Throwable? = null
    ) : CustomerAuthApiService {
        var capturedRequest: CustomerServiceRequestRequest? = null

        override suspend fun register(request: CustomerRegisterRequest): Response<CustomerRegisterResponse> =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun login(request: CustomerLoginRequest): Response<CustomerRegisterResponse> =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun getAddresses(): Response<List<CustomerAddressResponse>> =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun createAddress(request: CustomerAddressRequest): Response<CustomerAddressResponse> =
            throw UnsupportedOperationException("not used by this test")

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
        ): Response<CustomerServiceRequestResponse> {
            capturedRequest = request
            failure?.let { throw it }
            return response!!
        }

        override suspend fun getServiceRequests(): Response<CustomerServiceRequestListResponse> =
            throw UnsupportedOperationException("not used by this test")
    }

    @Test
    fun `sends the exact required fields, with preferred_time_to always null`() = runBlocking {
        val fake = FakeApiService(response = Response.success(CustomerServiceRequestResponse(101, "TCK-0101")))
        val repository = DefaultCustomerAuthRepository(fake)

        repository.createServiceRequest(
            serviceTypeId = 7L,
            customerAddressId = 42L,
            complaint = "Not Charging",
            preferredDate = "2026-09-25",
            preferredTimeFrom = "09:00",
            preferredTimeTo = null
        )

        assertEquals(
            CustomerServiceRequestRequest(
                serviceTypeId = 7L,
                customerAddressId = 42L,
                complaint = "Not Charging",
                preferredDate = "2026-09-25",
                preferredTimeFrom = "09:00",
                preferredTimeTo = null
            ),
            fake.capturedRequest
        )
    }

    @Test
    fun `successful 201 response is parsed into ServiceRequestResult Success`() = runBlocking {
        val fake = FakeApiService(response = Response.success(CustomerServiceRequestResponse(101, "TCK-0101")))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.createServiceRequest(7L, 42L, "Not Charging", "2026-09-25", "09:00", null)

        assertTrue(result is ServiceRequestResult.Success)
        val success = result as ServiceRequestResult.Success
        assertEquals(101L, success.response.id)
        assertEquals("TCK-0101", success.response.ticketNumber)
    }

    @Test
    fun `HTTP 422 surfaces the backend field error`() = runBlocking {
        val errorBody = """
            {"message":"The customer address id is invalid.","errors":{"customer_address_id":["The selected customer address id is invalid."]}}
        """.trimIndent().toResponseBody("application/json".toMediaType())
        val fake = FakeApiService(response = Response.error(422, errorBody))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.createServiceRequest(7L, 999L, "Not Charging", null, null, null)

        assertTrue(result is ServiceRequestResult.Error)
        val error = result as ServiceRequestResult.Error
        assertEquals(ApiErrorReason.VALIDATION_FAILED, error.reason)
        assertEquals(
            listOf("The selected customer address id is invalid."),
            error.fieldErrors?.get("customer_address_id")
        )
    }

    @Test
    fun `network failure maps to NETWORK_UNAVAILABLE`() = runBlocking {
        val fake = FakeApiService(failure = IOException("no connection"))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.createServiceRequest(7L, 42L, "Not Charging", null, null, null)

        assertTrue(result is ServiceRequestResult.Error)
        assertEquals(ApiErrorReason.NETWORK_UNAVAILABLE, (result as ServiceRequestResult.Error).reason)
    }
}
