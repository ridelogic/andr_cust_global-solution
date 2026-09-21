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
import com.arwe.newproject.data.remote.dto.CustomerServiceRequestStatusResponse
import com.arwe.newproject.data.remote.dto.CustomerServiceRequestSummaryResponse
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
 * Covers DefaultCustomerAuthRepository.getServiceRequests() only - the "data"-wrapped 200
 * response, an empty list, 401 mapping, and network-failure mapping - using a fake
 * CustomerAuthApiService so no real network call is ever made.
 */
class MyServicesRepositoryTest {

    private class FakeApiService(
        private val response: Response<CustomerServiceRequestListResponse>? = null,
        private val failure: Throwable? = null
    ) : CustomerAuthApiService {

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
        ): Response<CustomerServiceRequestResponse> =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun getServiceRequests(): Response<CustomerServiceRequestListResponse> {
            failure?.let { throw it }
            return response!!
        }
    }

    private fun summary(id: Long, serviceType: String) = CustomerServiceRequestSummaryResponse(
        id = id,
        ticketNumber = "TCK-0$id",
        serviceType = serviceType,
        status = CustomerServiceRequestStatusResponse(code = "PENDING", name = "Pending", isTerminal = false),
        priority = "NORMAL",
        preferredDate = "2026-09-25",
        preferredTimeFrom = "09:00",
        preferredTimeTo = null,
        createdAt = "2026-09-21T10:00:00+00:00"
    )

    @Test
    fun `successful response is parsed from the data envelope, newest first as the backend orders it`() = runBlocking {
        val fake = FakeApiService(
            response = Response.success(CustomerServiceRequestListResponse(data = listOf(summary(2, "Repair"), summary(1, "Not Charging"))))
        )
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.getServiceRequests()

        assertTrue(result is ServiceRequestListResult.Success)
        val success = result as ServiceRequestListResult.Success
        assertEquals(listOf(2L, 1L), success.serviceRequests.map { it.id })
    }

    @Test
    fun `an empty data array is parsed into an empty Success list`() = runBlocking {
        val fake = FakeApiService(response = Response.success(CustomerServiceRequestListResponse(data = emptyList())))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.getServiceRequests()

        assertTrue(result is ServiceRequestListResult.Success)
        assertTrue((result as ServiceRequestListResult.Success).serviceRequests.isEmpty())
    }

    @Test
    fun `HTTP 401 maps to UNAUTHORIZED`() = runBlocking {
        val errorBody = """{"message":"Unauthenticated."}""".toResponseBody("application/json".toMediaType())
        val fake = FakeApiService(response = Response.error(401, errorBody))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.getServiceRequests()

        assertTrue(result is ServiceRequestListResult.Error)
        assertEquals(ApiErrorReason.UNAUTHORIZED, (result as ServiceRequestListResult.Error).reason)
    }

    @Test
    fun `network failure maps to NETWORK_UNAVAILABLE`() = runBlocking {
        val fake = FakeApiService(failure = IOException("no connection"))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.getServiceRequests()

        assertTrue(result is ServiceRequestListResult.Error)
        assertEquals(ApiErrorReason.NETWORK_UNAVAILABLE, (result as ServiceRequestListResult.Error).reason)
    }
}
