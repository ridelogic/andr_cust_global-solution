package com.arwe.newproject.ui.servicedetails

import android.app.Application
import com.arwe.newproject.data.remote.dto.CustomerServiceRequestDetailResponse
import com.arwe.newproject.data.remote.dto.CustomerServiceRequestStatusResponse
import com.arwe.newproject.data.repository.AddressDeleteResult
import com.arwe.newproject.data.repository.AddressListResult
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceDetailsViewModelTest {

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
        var capturedId: Long? = null
        val getServiceRequestDetailDeferred = CompletableDeferred<ServiceRequestDetailResult>()

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

        override suspend fun getServiceRequests(): ServiceRequestListResult =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun getServiceRequestDetail(id: Long): ServiceRequestDetailResult {
            capturedId = id
            return getServiceRequestDetailDeferred.await()
        }
    }

    private fun detail(
        statusCode: String = "IN_PROGRESS",
        address: com.arwe.newproject.data.remote.dto.CustomerServiceRequestAddressResponse? = null,
        product: com.arwe.newproject.data.remote.dto.CustomerServiceRequestProductResponse? = null,
        technician: com.arwe.newproject.data.remote.dto.CustomerServiceRequestTechnicianResponse? = null,
        visits: List<com.arwe.newproject.data.remote.dto.CustomerServiceRequestVisitResponse>? = null
    ) = CustomerServiceRequestDetailResponse(
        id = 98,
        ticketNumber = "GS0998",
        source = "MOBILE_APP",
        priority = "NORMAL",
        status = CustomerServiceRequestStatusResponse(code = statusCode, name = statusCode, isTerminal = false),
        serviceType = "Repair",
        complaint = "Not Working",
        customerNotes = null,
        preferredDate = "2026-09-25",
        preferredTimeFrom = "09:00",
        preferredTimeTo = null,
        createdAt = "2026-09-20T10:00:00+00:00",
        closedAt = null,
        address = address,
        product = product,
        technician = technician,
        visits = visits
    )

    @Test
    fun `starts Loading before loadServiceRequestDetail is called`() {
        val viewModel = ServiceDetailsViewModel(Application(), FakeRepository())

        assertEquals(ServiceDetailsUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `loadServiceRequestDetail requests the exact given id and transitions Loading to Loaded`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = ServiceDetailsViewModel(Application(), fakeRepository)

        viewModel.loadServiceRequestDetail(98)
        assertEquals(ServiceDetailsUiState.Loading, viewModel.uiState.value)
        assertEquals(98L, fakeRepository.capturedId)

        val response = detail()
        fakeRepository.getServiceRequestDetailDeferred.complete(ServiceRequestDetailResult.Success(response))
        advanceUntilIdle()

        assertEquals(ServiceDetailsUiState.Loaded(response), viewModel.uiState.value)
    }

    @Test
    fun `null optional fields still produce a Loaded state, not an error`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = ServiceDetailsViewModel(Application(), fakeRepository)

        viewModel.loadServiceRequestDetail(98)
        val response = detail(address = null, product = null, technician = null, visits = null)
        fakeRepository.getServiceRequestDetailDeferred.complete(ServiceRequestDetailResult.Success(response))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ServiceDetailsUiState.Loaded)
        val loaded = state as ServiceDetailsUiState.Loaded
        assertNull(loaded.detail.address)
        assertNull(loaded.detail.product)
        assertNull(loaded.detail.technician)
        assertNull(loaded.detail.visits)
    }

    @Test
    fun `404 not found surfaces the exact backend copy`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = ServiceDetailsViewModel(Application(), fakeRepository)

        viewModel.loadServiceRequestDetail(12345)
        fakeRepository.getServiceRequestDetailDeferred.complete(
            ServiceRequestDetailResult.Error(reason = ApiErrorReason.NOT_FOUND, message = "Service request not found.")
        )
        advanceUntilIdle()

        assertEquals(ServiceDetailsUiState.Error("Service request not found."), viewModel.uiState.value)
    }

    @Test
    fun `403 forbidden (another customer's service request) surfaces a non-null error state`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = ServiceDetailsViewModel(Application(), fakeRepository)

        viewModel.loadServiceRequestDetail(7)
        fakeRepository.getServiceRequestDetailDeferred.complete(
            ServiceRequestDetailResult.Error(reason = ApiErrorReason.FORBIDDEN, message = "This action is unauthorized.")
        )
        advanceUntilIdle()

        assertEquals(ServiceDetailsUiState.Error("This action is unauthorized."), viewModel.uiState.value)
    }

    @Test
    fun `401 unauthorized surfaces a non-null error state`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = ServiceDetailsViewModel(Application(), fakeRepository)

        viewModel.loadServiceRequestDetail(7)
        fakeRepository.getServiceRequestDetailDeferred.complete(
            ServiceRequestDetailResult.Error(reason = ApiErrorReason.UNAUTHORIZED, message = "Your session has expired.")
        )
        advanceUntilIdle()

        assertEquals(ServiceDetailsUiState.Error("Your session has expired."), viewModel.uiState.value)
    }

    @Test
    fun `network failure surfaces a non-null error state`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = ServiceDetailsViewModel(Application(), fakeRepository)

        viewModel.loadServiceRequestDetail(98)
        fakeRepository.getServiceRequestDetailDeferred.complete(
            ServiceRequestDetailResult.Error(reason = ApiErrorReason.NETWORK_UNAVAILABLE, message = "No internet connection.")
        )
        advanceUntilIdle()

        assertEquals(ServiceDetailsUiState.Error("No internet connection."), viewModel.uiState.value)
    }
}
