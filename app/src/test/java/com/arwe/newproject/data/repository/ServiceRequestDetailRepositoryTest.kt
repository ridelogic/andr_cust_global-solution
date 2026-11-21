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
import com.arwe.newproject.data.remote.dto.CustomerServiceRequestStatusResponse
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
 * Covers DefaultCustomerAuthRepository.getServiceRequestDetail() only - 200 parsing (including
 * null optional fields), 404, 403, and network-failure mapping - using a fake
 * CustomerAuthApiService so no real network call is ever made.
 */
class ServiceRequestDetailRepositoryTest {

    private class FakeApiService(
        private val response: Response<CustomerServiceRequestDetailResponse>? = null,
        private val failure: Throwable? = null
    ) : CustomerAuthApiService {
        var capturedId: Long? = null

        override suspend fun register(request: CustomerRegisterRequest): Response<CustomerRegisterResponse> =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun login(request: CustomerLoginRequest): Response<CustomerRegisterResponse> =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun getAddresses(): Response<List<CustomerAddressResponse>> =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun createAddress(request: CustomerAddressRequest): Response<CustomerAddressResponse> =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun updateAddress(id: Long, request: CustomerAddressRequest): Response<CustomerAddressResponse> =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun deleteAddress(id: Long): Response<CustomerAddressDeleteResponse> =
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

        override suspend fun getServiceRequests(): Response<CustomerServiceRequestListResponse> =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun getServiceRequestDetail(id: Long): Response<CustomerServiceRequestDetailResponse> {
            capturedId = id
            failure?.let { throw it }
            return response!!
        }
    }

    private fun minimalDetail(statusCode: String = "IN_PROGRESS") = CustomerServiceRequestDetailResponse(
        id = 99,
        ticketNumber = "GS0998",
        source = "MOBILE_APP",
        priority = "NORMAL",
        status = CustomerServiceRequestStatusResponse(code = statusCode, name = "In Progress", isTerminal = false),
        serviceType = "Repair",
        complaint = "Not Working",
        customerNotes = null,
        preferredDate = "2026-09-25",
        preferredTimeFrom = "09:00",
        preferredTimeTo = null,
        createdAt = "2026-09-20T10:00:00+00:00",
        closedAt = null,
        address = null,
        product = null,
        technician = null,
        visits = null
    )

    @Test
    fun `requests the exact given id`() = runBlocking {
        val fake = FakeApiService(response = Response.success(minimalDetail()))
        val repository = DefaultCustomerAuthRepository(fake)

        repository.getServiceRequestDetail(99)

        assertEquals(99L, fake.capturedId)
    }

    @Test
    fun `successful 200 response is parsed into ServiceRequestDetailResult Success`() = runBlocking {
        val fake = FakeApiService(response = Response.success(minimalDetail()))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.getServiceRequestDetail(99)

        assertTrue(result is ServiceRequestDetailResult.Success)
        val success = result as ServiceRequestDetailResult.Success
        assertEquals("GS0998", success.response.ticketNumber)
        assertEquals("Repair", success.response.serviceType)
    }

    @Test
    fun `null optional fields (address, product, technician, visits) parse without error`() = runBlocking {
        val fake = FakeApiService(response = Response.success(minimalDetail()))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.getServiceRequestDetail(99)

        assertTrue(result is ServiceRequestDetailResult.Success)
        val detail = (result as ServiceRequestDetailResult.Success).response
        assertNull(detail.address)
        assertNull(detail.product)
        assertNull(detail.technician)
        assertNull(detail.visits)
    }

    @Test
    fun `HTTP 404 maps to NOT_FOUND`() = runBlocking {
        val errorBody = """{"message":"No query results for model."}"""
            .toResponseBody("application/json".toMediaType())
        val fake = FakeApiService(response = Response.error(404, errorBody))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.getServiceRequestDetail(12345)

        assertTrue(result is ServiceRequestDetailResult.Error)
        assertEquals(ApiErrorReason.NOT_FOUND, (result as ServiceRequestDetailResult.Error).reason)
    }

    @Test
    fun `HTTP 403 maps to FORBIDDEN, e g a service request owned by another customer`() = runBlocking {
        val errorBody = """{"message":"This action is unauthorized."}"""
            .toResponseBody("application/json".toMediaType())
        val fake = FakeApiService(response = Response.error(403, errorBody))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.getServiceRequestDetail(7)

        assertTrue(result is ServiceRequestDetailResult.Error)
        assertEquals(ApiErrorReason.FORBIDDEN, (result as ServiceRequestDetailResult.Error).reason)
    }

    @Test
    fun `HTTP 401 maps to UNAUTHORIZED`() = runBlocking {
        val errorBody = """{"message":"Unauthenticated."}""".toResponseBody("application/json".toMediaType())
        val fake = FakeApiService(response = Response.error(401, errorBody))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.getServiceRequestDetail(7)

        assertTrue(result is ServiceRequestDetailResult.Error)
        assertEquals(ApiErrorReason.UNAUTHORIZED, (result as ServiceRequestDetailResult.Error).reason)
    }

    @Test
    fun `network failure maps to NETWORK_UNAVAILABLE`() = runBlocking {
        val fake = FakeApiService(failure = IOException("no connection"))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.getServiceRequestDetail(99)

        assertTrue(result is ServiceRequestDetailResult.Error)
        assertEquals(ApiErrorReason.NETWORK_UNAVAILABLE, (result as ServiceRequestDetailResult.Error).reason)
    }
}
