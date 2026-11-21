package com.arwe.newproject.ui.activeservice

import android.app.Application
import com.arwe.newproject.data.remote.dto.CustomerServiceRequestStatusResponse
import com.arwe.newproject.data.remote.dto.CustomerServiceRequestSummaryResponse
import com.arwe.newproject.data.repository.AddressListResult
import com.arwe.newproject.data.repository.AddressDeleteResult
import com.arwe.newproject.data.repository.AddressResult
import com.arwe.newproject.data.repository.ApiErrorReason
import com.arwe.newproject.data.repository.CustomerAuthRepository
import com.arwe.newproject.data.repository.ProductBatteryTypeListResult
import com.arwe.newproject.data.repository.RegisterResult
import com.arwe.newproject.data.repository.SendOtpResult
import com.arwe.newproject.data.repository.ServiceRequestDetailResult
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

/**
 * "Active" here means status.is_terminal == false, matching the backend's own definition (see
 * ServiceStatusSeeder.php: only COMPLETED/CANCELLED/SERVICE_NOT_POSSIBLE are is_terminal=true) -
 * not a hardcoded status-code whitelist. These tests exercise that filtering, not
 * getServiceRequests() itself (already covered by MyServicesRepositoryTest, reused unchanged).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ActiveServiceViewModelTest {

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

        override suspend fun updateAddress(
            id: Long, addressType: String, addressLine1: String, addressLine2: String?, landmark: String?,
            city: String, state: String, postalCode: String, isDefault: Boolean
        ): AddressResult = throw UnsupportedOperationException("not used by this test")

        override suspend fun deleteAddress(id: Long): AddressDeleteResult =
            throw UnsupportedOperationException("not used by this test")

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

        override suspend fun getServiceRequestDetail(id: Long): ServiceRequestDetailResult =
            throw UnsupportedOperationException("not used by this test")
    }

    private fun summary(id: Long, statusCode: String, isTerminal: Boolean) = CustomerServiceRequestSummaryResponse(
        id = id,
        ticketNumber = "TCK-0$id",
        serviceType = "Repair",
        status = CustomerServiceRequestStatusResponse(code = statusCode, name = statusCode, isTerminal = isTerminal),
        priority = "NORMAL",
        preferredDate = "2026-09-25",
        preferredTimeFrom = "09:00",
        preferredTimeTo = null,
        createdAt = "2026-09-21T10:00:00+00:00"
    )

    @Test
    fun `only non-terminal-status requests are returned as active, terminal ones are filtered out`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = ActiveServiceViewModel(Application(), fakeRepository)

        viewModel.loadActiveServiceRequests()
        fakeRepository.getServiceRequestsDeferred.complete(
            ServiceRequestListResult.Success(
                listOf(
                    summary(1, "NEW", isTerminal = false),
                    summary(2, "COMPLETED", isTerminal = true),
                    summary(3, "IN_PROGRESS", isTerminal = false),
                    summary(4, "CANCELLED", isTerminal = true),
                    summary(5, "SERVICE_NOT_POSSIBLE", isTerminal = true)
                )
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ActiveServiceUiState.Loaded)
        assertEquals(listOf(1L, 3L), (state as ActiveServiceUiState.Loaded).serviceRequests.map { it.id })
    }

    @Test
    fun `when every request is terminal, the result is Empty, not Loaded with an empty list`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = ActiveServiceViewModel(Application(), fakeRepository)

        viewModel.loadActiveServiceRequests()
        fakeRepository.getServiceRequestsDeferred.complete(
            ServiceRequestListResult.Success(
                listOf(summary(1, "COMPLETED", isTerminal = true), summary(2, "CANCELLED", isTerminal = true))
            )
        )
        advanceUntilIdle()

        assertEquals(ActiveServiceUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `a genuinely empty backend list is also Empty`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = ActiveServiceViewModel(Application(), fakeRepository)

        viewModel.loadActiveServiceRequests()
        fakeRepository.getServiceRequestsDeferred.complete(ServiceRequestListResult.Success(emptyList()))
        advanceUntilIdle()

        assertEquals(ActiveServiceUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `an API error surfaces the backend message`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = ActiveServiceViewModel(Application(), fakeRepository)

        viewModel.loadActiveServiceRequests()
        fakeRepository.getServiceRequestsDeferred.complete(
            ServiceRequestListResult.Error(reason = ApiErrorReason.UNAUTHORIZED, message = "Your session has expired.")
        )
        advanceUntilIdle()

        assertEquals(ActiveServiceUiState.Error("Your session has expired."), viewModel.uiState.value)
    }

    @Test
    fun `a network failure surfaces a non-null error state`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = ActiveServiceViewModel(Application(), fakeRepository)

        viewModel.loadActiveServiceRequests()
        fakeRepository.getServiceRequestsDeferred.complete(
            ServiceRequestListResult.Error(reason = ApiErrorReason.NETWORK_UNAVAILABLE, message = "No internet connection.")
        )
        advanceUntilIdle()

        assertEquals(ActiveServiceUiState.Error("No internet connection."), viewModel.uiState.value)
    }
}
