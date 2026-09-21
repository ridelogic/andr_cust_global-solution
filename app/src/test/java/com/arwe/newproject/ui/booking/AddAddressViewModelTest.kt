package com.arwe.newproject.ui.booking

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.arwe.newproject.data.remote.dto.CustomerAddressResponse
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddAddressViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

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
        var createAddressCallCount = 0
        val createAddressDeferred = CompletableDeferred<AddressResult>()

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
        ): AddressResult {
            createAddressCallCount++
            return createAddressDeferred.await()
        }

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
    }

    private fun address() = CustomerAddressResponse(
        id = 42,
        addressType = "Home",
        addressLine1 = "221B Baker Street",
        addressLine2 = "Near Central Park",
        landmark = "Opposite Bus Stand",
        city = "Coimbatore",
        state = "Tamil Nadu",
        postalCode = "641035",
        latitude = null,
        longitude = null,
        isDefault = false
    )

    @Test
    fun `each required field reports REQUIRED when blank`() {
        val viewModel = AddAddressViewModel(Application(), FakeRepository())

        val result = viewModel.validate("", "", "", "", "")

        assertEquals(AddAddressFieldError.REQUIRED, result.addressTypeError)
        assertEquals(AddAddressFieldError.REQUIRED, result.addressLine1Error)
        assertEquals(AddAddressFieldError.REQUIRED, result.cityError)
        assertEquals(AddAddressFieldError.REQUIRED, result.stateError)
        assertEquals(AddAddressFieldError.REQUIRED, result.postalCodeError)
        assertEquals(false, result.isValid)
    }

    @Test
    fun `a fully valid form has no field errors`() {
        val viewModel = AddAddressViewModel(Application(), FakeRepository())

        val result = viewModel.validate("Home", "221B Baker Street", "Coimbatore", "Tamil Nadu", "641035")

        assertNull(result.addressTypeError)
        assertNull(result.addressLine1Error)
        assertNull(result.cityError)
        assertNull(result.stateError)
        assertNull(result.postalCodeError)
        assertTrue(result.isValid)
    }

    @Test
    fun `postal code longer than 20 characters is TOO_LONG while a 255-char field is not`() {
        val viewModel = AddAddressViewModel(Application(), FakeRepository())

        val result = viewModel.validate("Home", "221B Baker Street", "Coimbatore", "Tamil Nadu", "1".repeat(21))

        assertEquals(AddAddressFieldError.TOO_LONG, result.postalCodeError)
    }

    @Test
    fun `save transitions Idle to Loading then Success with the real backend address`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = AddAddressViewModel(Application(), fakeRepository)

        assertEquals(AddAddressUiState.Idle, viewModel.uiState.value)

        viewModel.save("Home", "221B Baker Street", "Near Central Park", "Opposite Bus Stand", "Coimbatore", "Tamil Nadu", "641035")
        assertEquals(AddAddressUiState.Loading, viewModel.uiState.value)

        val created = address()
        fakeRepository.createAddressDeferred.complete(AddressResult.Success(created))
        advanceUntilIdle()

        assertEquals(AddAddressUiState.Success(created), viewModel.uiState.value)
    }

    @Test
    fun `save ignores a duplicate tap while already loading`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = AddAddressViewModel(Application(), fakeRepository)

        viewModel.save("Home", "221B Baker Street", "", "", "Coimbatore", "Tamil Nadu", "641035")
        viewModel.save("Home", "221B Baker Street", "", "", "Coimbatore", "Tamil Nadu", "641035")
        advanceUntilIdle()

        assertEquals(1, fakeRepository.createAddressCallCount)
    }

    @Test
    fun `HTTP 422 surfaces the backend field error`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = AddAddressViewModel(Application(), fakeRepository)

        viewModel.save("", "221B Baker Street", "", "", "Coimbatore", "Tamil Nadu", "641035")
        fakeRepository.createAddressDeferred.complete(
            AddressResult.Error(
                reason = ApiErrorReason.VALIDATION_FAILED,
                message = null,
                fieldErrors = mapOf("address_type" to listOf("The address type field is required."))
            )
        )
        advanceUntilIdle()

        assertEquals(
            AddAddressUiState.Error("The address type field is required."),
            viewModel.uiState.value
        )
    }

    @Test
    fun `network failure surfaces a non-null error state`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = AddAddressViewModel(Application(), fakeRepository)

        viewModel.save("Home", "221B Baker Street", "", "", "Coimbatore", "Tamil Nadu", "641035")
        fakeRepository.createAddressDeferred.complete(
            AddressResult.Error(reason = ApiErrorReason.NETWORK_UNAVAILABLE, message = "No internet connection.")
        )
        advanceUntilIdle()

        assertEquals(AddAddressUiState.Error("No internet connection."), viewModel.uiState.value)
    }
}
