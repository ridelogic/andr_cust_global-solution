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
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/**
 * Covers DefaultCustomerAuthRepository.verifyOtp() only - both 200 response shapes, 422 mapping,
 * and network-failure mapping - using a fake CustomerAuthApiService so no real network call is
 * ever made.
 */
class VerifyOtpRepositoryTest {

    private class FakeApiService(
        private val response: Response<CustomerVerifyOtpResponse>? = null,
        private val failure: Throwable? = null
    ) : CustomerAuthApiService {
        var capturedRequest: CustomerVerifyOtpRequest? = null

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

        override suspend fun verifyOtp(request: CustomerVerifyOtpRequest): Response<CustomerVerifyOtpResponse> {
            capturedRequest = request
            failure?.let { throw it }
            return response!!
        }

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
    fun `correct OTP for an existing customer returns ExistingCustomer with token and customer`() = runBlocking {
        val customer = CustomerDto(
            id = 7, customerCode = "CUST-0007", name = "Vikram", mobile = "9791222882",
            alternateMobile = null, email = null
        )
        val fake = FakeApiService(
            response = Response.success(
                CustomerVerifyOtpResponse(
                    token = "plain-text-token",
                    customer = customer,
                    requiresRegistration = null,
                    mobile = null,
                    registrationToken = null,
                    message = null
                )
            )
        )
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.verifyOtp("9791222882", "307019")

        assertEquals(CustomerVerifyOtpRequest("9791222882", "307019"), fake.capturedRequest)
        assertTrue(result is VerifyOtpResult.ExistingCustomer)
        val success = result as VerifyOtpResult.ExistingCustomer
        assertEquals("plain-text-token", success.token)
        assertEquals(7L, success.customer.id)
        assertEquals("CUST-0007", success.customer.customerCode)
    }

    @Test
    fun `correct OTP for a new mobile returns RegistrationRequired`() = runBlocking {
        val fake = FakeApiService(
            response = Response.success(
                CustomerVerifyOtpResponse(
                    token = null,
                    customer = null,
                    requiresRegistration = true,
                    mobile = "9791222882",
                    registrationToken = "reg-token-abc",
                    message = "OTP verified. Customer registration is required."
                )
            )
        )
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.verifyOtp("9791222882", "307019")

        assertTrue(result is VerifyOtpResult.RegistrationRequired)
        val requiresRegistration = result as VerifyOtpResult.RegistrationRequired
        assertEquals("9791222882", requiresRegistration.mobile)
        assertEquals("reg-token-abc", requiresRegistration.registrationToken)
    }

    @Test
    fun `HTTP 422 invalid OTP surfaces the backend message`() = runBlocking {
        val errorBody = """{"message":"Invalid OTP."}"""
            .toResponseBody("application/json".toMediaType())
        val fake = FakeApiService(response = Response.error(422, errorBody))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.verifyOtp("9791222882", "000000")

        assertTrue(result is VerifyOtpResult.Error)
        val error = result as VerifyOtpResult.Error
        assertEquals(ApiErrorReason.VALIDATION_FAILED, error.reason)
        assertEquals("Invalid OTP.", error.message)
    }

    @Test
    fun `HTTP 422 expired OTP surfaces the backend message`() = runBlocking {
        val errorBody = """{"message":"OTP has expired. Please request a new one."}"""
            .toResponseBody("application/json".toMediaType())
        val fake = FakeApiService(response = Response.error(422, errorBody))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.verifyOtp("9791222882", "307019")

        assertTrue(result is VerifyOtpResult.Error)
        assertEquals("OTP has expired. Please request a new one.", (result as VerifyOtpResult.Error).message)
    }

    @Test
    fun `network failure maps to NETWORK_UNAVAILABLE`() = runBlocking {
        val fake = FakeApiService(failure = IOException("no connection"))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.verifyOtp("9791222882", "307019")

        assertTrue(result is VerifyOtpResult.Error)
        assertEquals(ApiErrorReason.NETWORK_UNAVAILABLE, (result as VerifyOtpResult.Error).reason)
    }
}
