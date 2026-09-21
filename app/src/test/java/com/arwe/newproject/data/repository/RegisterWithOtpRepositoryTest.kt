package com.arwe.newproject.data.repository

import com.arwe.newproject.data.remote.CustomerAuthApiService
import com.arwe.newproject.data.remote.dto.CustomerAddressRequest
import com.arwe.newproject.data.remote.dto.CustomerAddressResponse
import com.arwe.newproject.data.remote.dto.CustomerDto
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
 * Covers DefaultCustomerAuthRepository.registerWithOtp() only - request construction (including
 * omitted email), 201 parsing, 422 mapping, and network-failure mapping - using a fake
 * CustomerAuthApiService so no real network call is ever made.
 */
class RegisterWithOtpRepositoryTest {

    private class FakeApiService(
        private val response: Response<CustomerRegisterResponse>? = null,
        private val failure: Throwable? = null
    ) : CustomerAuthApiService {
        var capturedRequest: CustomerRegisterWithOtpRequest? = null

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

        override suspend fun registerWithOtp(
            request: CustomerRegisterWithOtpRequest
        ): Response<CustomerRegisterResponse> {
            capturedRequest = request
            failure?.let { throw it }
            return response!!
        }

        override suspend fun getBatteryTypes(): Response<List<ProductBatteryTypeResponse>> =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun createServiceRequest(
            request: CustomerServiceRequestRequest
        ): Response<CustomerServiceRequestResponse> =
            throw UnsupportedOperationException("not used by this test")
    }

    @Test
    fun `valid registration request is parsed into RegisterResult Success on 201`() = runBlocking {
        val customer = CustomerDto(
            id = 8, customerCode = "CUST-0008", name = "Test Customer", mobile = "9791222882",
            alternateMobile = null, email = "testcustomer2026@gmail.com"
        )
        // Response.success() defaults to HTTP 200 - the repository treats any 2xx as successful
        // via isSuccessful, so the exact status code (201 in the real backend) is not this test's
        // concern; response *parsing* is.
        val fake = FakeApiService(
            response = Response.success(CustomerRegisterResponse("plain-text-token", customer))
        )
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.registerWithOtp(
            mobile = "9791222882",
            name = "Test Customer",
            email = "testcustomer2026@gmail.com",
            registrationToken = "reg-token-abc"
        )

        assertEquals(
            CustomerRegisterWithOtpRequest("9791222882", "Test Customer", "testcustomer2026@gmail.com", "reg-token-abc"),
            fake.capturedRequest
        )
        assertTrue(result is RegisterResult.Success)
        val success = result as RegisterResult.Success
        assertEquals("plain-text-token", success.token)
        assertEquals(8L, success.customer.id)
    }

    @Test
    fun `optional email omitted still sends a valid request with a null email`() = runBlocking {
        val customer = CustomerDto(
            id = 9, customerCode = "CUST-0009", name = "No Email Customer", mobile = "9791222883",
            alternateMobile = null, email = null
        )
        val fake = FakeApiService(
            response = Response.success(CustomerRegisterResponse("plain-text-token", customer))
        )
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.registerWithOtp(
            mobile = "9791222883",
            name = "No Email Customer",
            email = null,
            registrationToken = "reg-token-xyz"
        )

        assertNull(fake.capturedRequest?.email)
        assertTrue(result is RegisterResult.Success)
    }

    @Test
    fun `HTTP 422 duplicate account surfaces the backend message`() = runBlocking {
        val errorBody = """{"message":"An account already exists for this mobile number."}"""
            .toResponseBody("application/json".toMediaType())
        val fake = FakeApiService(response = Response.error(422, errorBody))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.registerWithOtp("9791222882", "Test Customer", null, "reg-token-abc")

        assertTrue(result is RegisterResult.Error)
        val error = result as RegisterResult.Error
        assertEquals(ApiErrorReason.VALIDATION_FAILED, error.reason)
        assertEquals("An account already exists for this mobile number.", error.message)
    }

    @Test
    fun `network failure maps to NETWORK_UNAVAILABLE`() = runBlocking {
        val fake = FakeApiService(failure = IOException("no connection"))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.registerWithOtp("9791222882", "Test Customer", null, "reg-token-abc")

        assertTrue(result is RegisterResult.Error)
        assertEquals(ApiErrorReason.NETWORK_UNAVAILABLE, (result as RegisterResult.Error).reason)
    }
}
