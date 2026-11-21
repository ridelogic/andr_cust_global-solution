package com.arwe.newproject.ui.addresses

import android.app.Application
import com.arwe.newproject.data.remote.dto.CustomerAddressResponse
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyAddressesViewModelTest {

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
        var getAddressesCallCount = 0
        var getAddressesDeferred = CompletableDeferred<AddressListResult>()

        var capturedDeleteId: Long? = null
        val deleteAddressDeferred = CompletableDeferred<AddressDeleteResult>()

        var capturedSetDefaultId: Long? = null
        var capturedSetDefaultIsDefault: Boolean? = null
        val updateAddressDeferred = CompletableDeferred<AddressResult>()

        override suspend fun register(
            name: String, mobile: String, email: String, password: String, passwordConfirmation: String
        ): RegisterResult = throw UnsupportedOperationException("not used by this test")

        override suspend fun login(email: String, password: String): RegisterResult =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun getAddresses(): AddressListResult {
            getAddressesCallCount++
            return getAddressesDeferred.await()
        }

        override suspend fun createAddress(
            addressType: String, addressLine1: String, addressLine2: String?, landmark: String?,
            city: String, state: String, postalCode: String
        ): AddressResult = throw UnsupportedOperationException("not used by this test")

        override suspend fun updateAddress(
            id: Long, addressType: String, addressLine1: String, addressLine2: String?, landmark: String?,
            city: String, state: String, postalCode: String, isDefault: Boolean
        ): AddressResult {
            capturedSetDefaultId = id
            capturedSetDefaultIsDefault = isDefault
            return updateAddressDeferred.await()
        }

        override suspend fun deleteAddress(id: Long): AddressDeleteResult {
            capturedDeleteId = id
            return deleteAddressDeferred.await()
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

        override suspend fun getServiceRequests(): ServiceRequestListResult =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun getServiceRequestDetail(id: Long): ServiceRequestDetailResult =
            throw UnsupportedOperationException("not used by this test")
    }

    private fun address(id: Long = 42, isDefault: Boolean = false) = CustomerAddressResponse(
        id = id,
        addressType = "Home",
        addressLine1 = "221B Baker Street",
        addressLine2 = "Near Central Park",
        landmark = "Opposite Bus Stand",
        city = "Coimbatore",
        state = "Tamil Nadu",
        postalCode = "641035",
        latitude = null,
        longitude = null,
        isDefault = isDefault
    )

    @Test
    fun `loadAddresses starts Loading and transitions to Loaded with a non-empty list`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = MyAddressesViewModel(Application(), fakeRepository)

        viewModel.loadAddresses()
        assertEquals(MyAddressesUiState.Loading, viewModel.uiState.value)

        fakeRepository.getAddressesDeferred.complete(AddressListResult.Success(listOf(address(1), address(42))))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is MyAddressesUiState.Loaded)
        assertEquals(listOf(1L, 42L), (state as MyAddressesUiState.Loaded).addresses.map { it.id })
    }

    @Test
    fun `loadAddresses with an empty backend list transitions to Empty, not Loaded`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = MyAddressesViewModel(Application(), fakeRepository)

        viewModel.loadAddresses()
        fakeRepository.getAddressesDeferred.complete(AddressListResult.Success(emptyList()))
        advanceUntilIdle()

        assertEquals(MyAddressesUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `loadAddresses surfaces an API error`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = MyAddressesViewModel(Application(), fakeRepository)

        viewModel.loadAddresses()
        fakeRepository.getAddressesDeferred.complete(
            AddressListResult.Error(reason = ApiErrorReason.UNAUTHORIZED, message = "Your session has expired.")
        )
        advanceUntilIdle()

        assertEquals(MyAddressesUiState.Error("Your session has expired."), viewModel.uiState.value)
    }

    @Test
    fun `loadAddresses surfaces a network failure`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = MyAddressesViewModel(Application(), fakeRepository)

        viewModel.loadAddresses()
        fakeRepository.getAddressesDeferred.complete(
            AddressListResult.Error(reason = ApiErrorReason.NETWORK_UNAVAILABLE, message = "No internet connection.")
        )
        advanceUntilIdle()

        assertEquals(MyAddressesUiState.Error("No internet connection."), viewModel.uiState.value)
    }

    @Test
    fun `deleteAddress success transitions actionState back to Idle and reloads the list`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = MyAddressesViewModel(Application(), fakeRepository)

        viewModel.deleteAddress(42)
        assertEquals(MyAddressesActionState.InProgress, viewModel.actionState.value)
        assertEquals(42L, fakeRepository.capturedDeleteId)

        fakeRepository.deleteAddressDeferred.complete(AddressDeleteResult.Success)
        fakeRepository.getAddressesDeferred.complete(AddressListResult.Success(emptyList()))
        advanceUntilIdle()

        assertEquals(MyAddressesActionState.Idle, viewModel.actionState.value)
        assertEquals(1, fakeRepository.getAddressesCallCount)
        assertEquals(MyAddressesUiState.Empty, viewModel.uiState.value)
    }

    @Test
    fun `deleteAddress error surfaces a non-null actionState message without reloading`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = MyAddressesViewModel(Application(), fakeRepository)

        viewModel.deleteAddress(42)
        fakeRepository.deleteAddressDeferred.complete(
            AddressDeleteResult.Error(reason = ApiErrorReason.NETWORK_UNAVAILABLE, message = "No internet connection.")
        )
        advanceUntilIdle()

        assertEquals(MyAddressesActionState.Error("No internet connection."), viewModel.actionState.value)
        assertEquals(0, fakeRepository.getAddressesCallCount)
    }

    @Test
    fun `setDefault sends the address's own current fields with isDefault true and reloads on success`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = MyAddressesViewModel(Application(), fakeRepository)
        val target = address(id = 7, isDefault = false)

        viewModel.setDefault(target)
        assertEquals(MyAddressesActionState.InProgress, viewModel.actionState.value)
        assertEquals(7L, fakeRepository.capturedSetDefaultId)
        assertEquals(true, fakeRepository.capturedSetDefaultIsDefault)

        fakeRepository.updateAddressDeferred.complete(AddressResult.Success(target.copy(isDefault = true)))
        fakeRepository.getAddressesDeferred.complete(AddressListResult.Success(listOf(target.copy(isDefault = true))))
        advanceUntilIdle()

        assertEquals(MyAddressesActionState.Idle, viewModel.actionState.value)
        assertEquals(1, fakeRepository.getAddressesCallCount)
    }

    @Test
    fun `setDefault error surfaces a non-null actionState message`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = MyAddressesViewModel(Application(), fakeRepository)

        viewModel.setDefault(address(id = 7))
        fakeRepository.updateAddressDeferred.complete(
            AddressResult.Error(reason = ApiErrorReason.SERVER_ERROR, message = "Something went wrong on our end. Please try again later")
        )
        advanceUntilIdle()

        assertEquals(MyAddressesActionState.Error("Something went wrong on our end. Please try again later"), viewModel.actionState.value)
    }
}
