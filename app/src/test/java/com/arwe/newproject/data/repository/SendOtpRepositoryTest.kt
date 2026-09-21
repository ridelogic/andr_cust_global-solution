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
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/**
 * Covers DefaultCustomerAuthRepository.sendOtp() only - request construction, 200 parsing, 422
 * mapping, and network-failure mapping - using a fake CustomerAuthApiService so no real network
 * call is ever made.
 */
class SendOtpRepositoryTest {

    private class FakeApiService(
        private val response: Response<CustomerSendOtpResponse>? = null,
        private val failure: Throwable? = null
    ) : CustomerAuthApiService {
        var capturedRequest: CustomerSendOtpRequest? = null

        override suspend fun register(request: CustomerRegisterRequest): Response<CustomerRegisterResponse> =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun login(request: CustomerLoginRequest): Response<CustomerRegisterResponse> =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun getAddresses(): Response<List<CustomerAddressResponse>> =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun createAddress(request: CustomerAddressRequest): Response<CustomerAddressResponse> =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun sendOtp(request: CustomerSendOtpRequest): Response<CustomerSendOtpResponse> {
            capturedRequest = request
            failure?.let { throw it }
            return response!!
        }

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

    @Test
    fun `valid mobile calls send-otp with that exact mobile`() = runBlocking {
        val fake = FakeApiService(
            response = Response.success(CustomerSendOtpResponse("OTP generated successfully", "307019"))
        )
        val repository = DefaultCustomerAuthRepository(fake)

        repository.sendOtp("9791222882")

        assertEquals(CustomerSendOtpRequest("9791222882"), fake.capturedRequest)
    }

    @Test
    fun `successful 200 response is parsed into SendOtpResult Success`() = runBlocking {
        val fake = FakeApiService(
            response = Response.success(CustomerSendOtpResponse("OTP generated successfully", "307019"))
        )
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.sendOtp("9791222882")

        assertTrue(result is SendOtpResult.Success)
        val success = result as SendOtpResult.Success
        assertEquals("OTP generated successfully", success.response.message)
        assertEquals("307019", success.response.otp)
    }

    @Test
    fun `HTTP 422 maps to VALIDATION_FAILED with the backend field message`() = runBlocking {
        val errorBody = """
            {"message":"The mobile field is required.","errors":{"mobile":["The mobile field is required."]}}
        """.trimIndent().toResponseBody("application/json".toMediaType())
        val fake = FakeApiService(response = Response.error(422, errorBody))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.sendOtp("")

        assertTrue(result is SendOtpResult.Error)
        val error = result as SendOtpResult.Error
        assertEquals(ApiErrorReason.VALIDATION_FAILED, error.reason)
        assertEquals(listOf("The mobile field is required."), error.fieldErrors?.get("mobile"))
    }

    @Test
    fun `network failure maps to NETWORK_UNAVAILABLE`() = runBlocking {
        val fake = FakeApiService(failure = IOException("no connection"))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.sendOtp("9791222882")

        assertTrue(result is SendOtpResult.Error)
        assertEquals(ApiErrorReason.NETWORK_UNAVAILABLE, (result as SendOtpResult.Error).reason)
    }
}
