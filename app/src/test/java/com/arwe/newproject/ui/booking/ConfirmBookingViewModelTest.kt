package com.arwe.newproject.ui.booking

import android.app.Application
import com.arwe.newproject.data.remote.dto.CustomerServiceRequestResponse
import com.arwe.newproject.data.repository.AddressListResult
import com.arwe.newproject.data.repository.AddressResult
import com.arwe.newproject.data.repository.ApiErrorReason
import com.arwe.newproject.data.repository.CustomerAuthRepository
import com.arwe.newproject.data.repository.ProductBatteryTypeListResult
import com.arwe.newproject.data.repository.RegisterResult
import com.arwe.newproject.data.repository.SendOtpResult
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
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConfirmBookingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val dummyProduct = Product(id = "car_battery", titleRes = 0, descriptionRes = 0, iconRes = 0)

    private fun batteryProblem(id: String): Problem =
        ProblemCatalog.problemsFor(ServiceCategory.BATTERY, dummyProduct).first { it.id == id }

    private class FakeRepository : CustomerAuthRepository {
        var createServiceRequestCalled = false
        var capturedServiceTypeId: Long? = null
        var capturedComplaint: String? = null
        val createServiceRequestDeferred = CompletableDeferred<ServiceRequestResult>()

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
        ): ServiceRequestResult {
            createServiceRequestCalled = true
            capturedServiceTypeId = serviceTypeId
            capturedComplaint = complaint
            return createServiceRequestDeferred.await()
        }
    }

    @Test
    fun `a non-BATTERY category never calls the repository and reports UnsupportedCategory`() {
        val fakeRepository = FakeRepository()
        val viewModel = ConfirmBookingViewModel(Application(), fakeRepository)
        val problem = ProblemCatalog.problemsFor(ServiceCategory.HOME_APPLIANCE, dummyProduct).first()

        viewModel.confirmBooking(
            category = ServiceCategory.HOME_APPLIANCE,
            problem = problem,
            customerAddressId = 42L,
            preferredDate = "2026-09-25",
            preferredTimeFrom = "09:00"
        )

        assertFalse(fakeRepository.createServiceRequestCalled)
        assertEquals(ConfirmBookingUiState.UnsupportedCategory, viewModel.uiState.value)
    }

    @Test
    fun `a missing address id never calls the repository and reports MissingAddress`() {
        val fakeRepository = FakeRepository()
        val viewModel = ConfirmBookingViewModel(Application(), fakeRepository)

        viewModel.confirmBooking(
            category = ServiceCategory.BATTERY,
            problem = batteryProblem("not_charging"),
            customerAddressId = null,
            preferredDate = "2026-09-25",
            preferredTimeFrom = "09:00"
        )

        assertFalse(fakeRepository.createServiceRequestCalled)
        assertEquals(ConfirmBookingUiState.MissingAddress, viewModel.uiState.value)
    }

    @Test
    fun `BATTERY with a valid address transitions Idle to Loading then Success, using the mapped service_type_id and complaint`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = ConfirmBookingViewModel(Application(), fakeRepository)

        assertEquals(ConfirmBookingUiState.Idle, viewModel.uiState.value)

        viewModel.confirmBooking(
            category = ServiceCategory.BATTERY,
            problem = batteryProblem("not_charging"),
            customerAddressId = 42L,
            preferredDate = "2026-09-25",
            preferredTimeFrom = "09:00"
        )
        assertEquals(ConfirmBookingUiState.Loading, viewModel.uiState.value)
        assertEquals(7L, fakeRepository.capturedServiceTypeId)
        assertEquals("Not Charging", fakeRepository.capturedComplaint)

        val response = CustomerServiceRequestResponse(101, "TCK-0101")
        fakeRepository.createServiceRequestDeferred.complete(ServiceRequestResult.Success(response))
        advanceUntilIdle()

        assertEquals(ConfirmBookingUiState.Success(response), viewModel.uiState.value)
    }

    @Test
    fun `confirmBooking ignores a duplicate tap while already loading`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = ConfirmBookingViewModel(Application(), fakeRepository)

        viewModel.confirmBooking(ServiceCategory.BATTERY, batteryProblem("other"), 42L, null, null)
        viewModel.confirmBooking(ServiceCategory.BATTERY, batteryProblem("other"), 42L, null, null)
        advanceUntilIdle()

        assertEquals(1, listOf(fakeRepository.createServiceRequestCalled).count { it })
    }

    @Test
    fun `HTTP 422 surfaces the backend field error`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = ConfirmBookingViewModel(Application(), fakeRepository)

        viewModel.confirmBooking(ServiceCategory.BATTERY, batteryProblem("low_backup"), 42L, null, null)
        fakeRepository.createServiceRequestDeferred.complete(
            ServiceRequestResult.Error(
                reason = ApiErrorReason.VALIDATION_FAILED,
                message = null,
                fieldErrors = mapOf("customer_address_id" to listOf("The selected customer address id is invalid."))
            )
        )
        advanceUntilIdle()

        assertEquals(
            ConfirmBookingUiState.Error("The selected customer address id is invalid."),
            viewModel.uiState.value
        )
    }

    @Test
    fun `network failure surfaces a non-null error state`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = ConfirmBookingViewModel(Application(), fakeRepository)

        viewModel.confirmBooking(ServiceCategory.BATTERY, batteryProblem("dead_battery"), 42L, null, null)
        fakeRepository.createServiceRequestDeferred.complete(
            ServiceRequestResult.Error(reason = ApiErrorReason.NETWORK_UNAVAILABLE, message = "No internet connection.")
        )
        advanceUntilIdle()

        assertEquals(ConfirmBookingUiState.Error("No internet connection."), viewModel.uiState.value)
    }
}
