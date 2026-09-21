package com.arwe.newproject.ui.booking

import android.app.Application
import com.arwe.newproject.data.remote.dto.ProductBatteryTypeResponse
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductSelectionViewModelTest {

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
        var getBatteryTypesCalled = false
        val batteryTypesDeferred = CompletableDeferred<ProductBatteryTypeListResult>()

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

        override suspend fun getBatteryTypes(): ProductBatteryTypeListResult {
            getBatteryTypesCalled = true
            return batteryTypesDeferred.await()
        }

        override suspend fun createServiceRequest(
            serviceTypeId: Long, customerAddressId: Long, complaint: String,
            preferredDate: String?, preferredTimeFrom: String?, preferredTimeTo: String?
        ): ServiceRequestResult = throw UnsupportedOperationException("not used by this test")
    }

    @Test
    fun `non-battery categories load the existing static catalog without calling the repository`() {
        val fakeRepository = FakeRepository()
        val viewModel = ProductSelectionViewModel(Application(), fakeRepository)

        viewModel.loadProducts(ServiceCategory.HOME_APPLIANCE)

        assertFalse(fakeRepository.getBatteryTypesCalled)
        val state = viewModel.uiState.value
        assertTrue(state is ProductListUiState.Loaded)
        assertEquals(ProductCatalog.productsFor(ServiceCategory.HOME_APPLIANCE), (state as ProductListUiState.Loaded).products)
    }

    @Test
    fun `BATTERY category transitions Loading to Loaded mapped from the API response`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = ProductSelectionViewModel(Application(), fakeRepository)

        viewModel.loadProducts(ServiceCategory.BATTERY)
        assertEquals(ProductListUiState.Loading, viewModel.uiState.value)
        assertTrue(fakeRepository.getBatteryTypesCalled)

        fakeRepository.batteryTypesDeferred.complete(
            ProductBatteryTypeListResult.Success(
                listOf(
                    ProductBatteryTypeResponse(1, "Car Battery", "CAR_BATTERY"),
                    ProductBatteryTypeResponse(2, "Bike Battery", "BIKE_BATTERY"),
                    ProductBatteryTypeResponse(3, "UPS Battery", "UPS_BATTERY"),
                    ProductBatteryTypeResponse(4, "Other Battery", "OTHER_BATTERY")
                )
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ProductListUiState.Loaded)
        val ids = (state as ProductListUiState.Loaded).products.map { it.id }
        assertEquals(listOf("car_battery", "bike_battery", "ups_battery", "other_battery"), ids)
    }

    @Test
    fun `an unrecognized battery code from the API is silently skipped, never shown as a fifth type`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = ProductSelectionViewModel(Application(), fakeRepository)

        viewModel.loadProducts(ServiceCategory.BATTERY)
        fakeRepository.batteryTypesDeferred.complete(
            ProductBatteryTypeListResult.Success(
                listOf(
                    ProductBatteryTypeResponse(1, "Car Battery", "CAR_BATTERY"),
                    ProductBatteryTypeResponse(5, "Mobile Phone Battery", "MOBILE_PHONE_BATTERY")
                )
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value as ProductListUiState.Loaded
        assertEquals(listOf("car_battery"), state.products.map { it.id })
    }

    @Test
    fun `HTTP error while loading battery types surfaces an Error state`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = ProductSelectionViewModel(Application(), fakeRepository)

        viewModel.loadProducts(ServiceCategory.BATTERY)
        fakeRepository.batteryTypesDeferred.complete(
            ProductBatteryTypeListResult.Error(
                reason = ApiErrorReason.SERVER_ERROR,
                message = "Something went wrong on our end. Please try again later"
            )
        )
        advanceUntilIdle()

        assertEquals(
            ProductListUiState.Error("Something went wrong on our end. Please try again later"),
            viewModel.uiState.value
        )
    }
}
