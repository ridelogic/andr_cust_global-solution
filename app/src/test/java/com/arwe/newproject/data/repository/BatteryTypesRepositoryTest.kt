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
 * Covers DefaultCustomerAuthRepository.getBatteryTypes() only - 200 parsing, 401/422 mapping, and
 * network-failure mapping - using a fake CustomerAuthApiService so no real network call is ever
 * made.
 */
class BatteryTypesRepositoryTest {

    private class FakeApiService(
        private val response: Response<List<ProductBatteryTypeResponse>>? = null,
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

        override suspend fun getBatteryTypes(): Response<List<ProductBatteryTypeResponse>> {
            failure?.let { throw it }
            return response!!
        }

        override suspend fun createServiceRequest(
            request: CustomerServiceRequestRequest
        ): Response<CustomerServiceRequestResponse> =
            throw UnsupportedOperationException("not used by this test")
    }

    @Test
    fun `successful 200 response is parsed into ProductBatteryTypeListResult Success`() = runBlocking {
        val fake = FakeApiService(
            response = Response.success(
                listOf(
                    ProductBatteryTypeResponse(1, "Car Battery", "CAR_BATTERY"),
                    ProductBatteryTypeResponse(2, "Bike Battery", "BIKE_BATTERY"),
                    ProductBatteryTypeResponse(3, "UPS Battery", "UPS_BATTERY"),
                    ProductBatteryTypeResponse(4, "Other Battery", "OTHER_BATTERY")
                )
            )
        )
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.getBatteryTypes()

        assertTrue(result is ProductBatteryTypeListResult.Success)
        val success = result as ProductBatteryTypeListResult.Success
        assertEquals(4, success.batteryTypes.size)
        assertEquals("CAR_BATTERY", success.batteryTypes[0].code)
    }

    @Test
    fun `HTTP 401 maps to UNAUTHORIZED`() = runBlocking {
        val errorBody = """{"message":"Unauthenticated."}""".toResponseBody("application/json".toMediaType())
        val fake = FakeApiService(response = Response.error(401, errorBody))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.getBatteryTypes()

        assertTrue(result is ProductBatteryTypeListResult.Error)
        assertEquals(ApiErrorReason.UNAUTHORIZED, (result as ProductBatteryTypeListResult.Error).reason)
    }

    @Test
    fun `network failure maps to NETWORK_UNAVAILABLE`() = runBlocking {
        val fake = FakeApiService(failure = IOException("no connection"))
        val repository = DefaultCustomerAuthRepository(fake)

        val result = repository.getBatteryTypes()

        assertTrue(result is ProductBatteryTypeListResult.Error)
        assertEquals(ApiErrorReason.NETWORK_UNAVAILABLE, (result as ProductBatteryTypeListResult.Error).reason)
    }
}
