package com.arwe.newproject.ui.myservices

import android.app.Application
import com.arwe.newproject.data.remote.dto.CustomerServiceRequestStatusResponse
import com.arwe.newproject.data.remote.dto.CustomerServiceRequestSummaryResponse
import com.arwe.newproject.data.repository.AddressListResult
import com.arwe.newproject.data.repository.AddressResult
import com.arwe.newproject.data.repository.ApiErrorReason
import com.arwe.newproject.data.repository.CustomerAuthRepository
import com.arwe.newproject.data.repository.ProductBatteryTypeListResult
import com.arwe.newproject.data.repository.RegisterResult
import com.arwe.newproject.data.repository.SendOtpResult
import com.arwe.newproject.data.repository.ServiceRequestListResult
import com.arwe.newproject.data.repository.ServiceRequestResult
import com.arwe.newproject.data.repository.VerifyOtpResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyServicesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeRepository : CustomerAuthRepository {
        val getServiceRequestsDeferred = CompletableDeferred<ServiceRequestListResult>()

        override suspend fun register(
            name: String, mobile: String, email: String, password: String, passwordConfirmation: String
        ): RegisterResult = throw UnsupportedOperationException("not used by this test")

        override suspend fun login(email: String, password: String): RegisterResult =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun getAddresses(): AddressListResult =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun createAddress(
            addressType: String, addressLine1: String, addressLine2: String?, landmark: String?,
            city: String, state: String, postalCode: String
        ): AddressResult = throw UnsupportedOperationException("not used by this test")

        override suspend fun sendOtp(mobile: String): SendOtpResult =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun verifyOtp(mobile: String, otp: String): VerifyOtpResult =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun registerWithOtp(
            mobile: String, name: String, email: String?, registrationToken: String
        ): RegisterResult = throw UnsupportedOperationException("not used by this test")

        override suspend fun getBatteryTypes(): ProductBatteryTypeListResult =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun createServiceRequest(
            serviceTypeId: Long, customerAddressId: Long, complaint: String,
            preferredDate: String?, preferredTimeFrom: String?, preferredTimeTo: String?
        ): ServiceRequestResult = throw UnsupportedOperationException("not used by this test")

        override suspend fun getServiceRequests(): ServiceRequestListResult = getServiceRequestsDeferred.await()
    }

    private fun summary(id: Long) = CustomerServiceRequestSummaryResponse(
        id = id,
        ticketNumber = "TCK-0$id",
        serviceType = "Repair",
        status = CustomerServiceRequestStatusResponse(code = "PENDING", name = "Pending", isTerminal = false),
        priority = "NORMAL",
        preferredDate = "2026-09-25",
        preferredTimeFrom = "09:00",
        preferredTimeTo = null,
        createdAt = "2026-09-21T10:00:00+00:00"
    )

    @Test
    fun `starts Loading and transitions to Loaded with a non-empty list`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = MyServicesViewModel(Application(), fakeRepository)

        assertEquals(MyServicesUiState.Loading, viewModel.uiState.value)

        viewModel.loadServiceRequests()
        assertEquals(MyServicesUiState.Loading, viewModel.uiState.value)

        fakeRepository.getServiceRequestsDeferred.complete(ServiceRequestListResult.Success(listOf(summary(1), summary(2))))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is MyServicesUiState.Loaded)
        assertEquals(listOf(1L, 2L), (state as MyServicesUiState.Loaded).serviceRequests.map { it.id })
    }

    @Test
    fun `an empty successful list transitions to Empty, not Loaded`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = MyServicesViewModel(Application(), fakeRepository)

        viewModel.loadServiceRequests()
        fakeRepository.getServiceRequestsDeferred.complete(ServiceRequestListResult.Success(emptyList()))
        advanceUntilIdle()

        assertEquals(MyServicesUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `an API error surfaces the backend message`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = MyServicesViewModel(Application(), fakeRepository)

        viewModel.loadServiceRequests()
        fakeRepository.getServiceRequestsDeferred.complete(
            ServiceRequestListResult.Error(reason = ApiErrorReason.UNAUTHORIZED, message = "Your session has expired.")
        )
        advanceUntilIdle()

        assertEquals(MyServicesUiState.Error("Your session has expired."), viewModel.uiState.value)
    }

    @Test
    fun `a network failure surfaces a non-null error state`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = MyServicesViewModel(Application(), fakeRepository)

        viewModel.loadServiceRequests()
        fakeRepository.getServiceRequestsDeferred.complete(
            ServiceRequestListResult.Error(reason = ApiErrorReason.NETWORK_UNAVAILABLE, message = "No internet connection.")
        )
        advanceUntilIdle()

        assertEquals(MyServicesUiState.Error("No internet connection."), viewModel.uiState.value)
    }
}
