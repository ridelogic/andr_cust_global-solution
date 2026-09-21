package com.arwe.newproject.ui.login

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.arwe.newproject.data.remote.dto.CustomerSendOtpResponse
import com.arwe.newproject.data.repository.ApiErrorReason
import com.arwe.newproject.data.repository.CustomerAuthRepository
import com.arwe.newproject.data.repository.AddressListResult
import com.arwe.newproject.data.repository.AddressResult
import com.arwe.newproject.data.repository.RegisterResult
import com.arwe.newproject.data.repository.SendOtpResult
import com.arwe.newproject.data.repository.ProductBatteryTypeListResult
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
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    // LiveData.setValue() (used by LoginViewModel.validate()) asserts it's running on the main
    // thread; this rule swaps in a synchronous ArchTaskExecutor so that check passes in a plain
    // JVM unit test without needing Robolectric.
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // viewModelScope.launch resolves Dispatchers.Main.immediate directly (not just whatever
    // runTest's own TestScope uses) - it must be explicitly installed, or the launch call itself
    // throws before ever reaching the fake repository.
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
        var sendOtpCalled = false
        var capturedMobile: String? = null
        val sendOtpDeferred = CompletableDeferred<SendOtpResult>()

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

        override suspend fun sendOtp(mobile: String): SendOtpResult {
            sendOtpCalled = true
            capturedMobile = mobile
            return sendOtpDeferred.await()
        }

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

    @Test
    fun `blank mobile fails validation`() {
        val viewModel = LoginViewModel(Application(), FakeRepository())

        val result = viewModel.validate("   ")

        assertEquals(LoginFieldError.REQUIRED, result.mobileError)
        assertEquals(false, result.isValid)
    }

    @Test
    fun `well-formed mobile passes validation`() {
        val viewModel = LoginViewModel(Application(), FakeRepository())

        val result = viewModel.validate("9791222882")

        assertNull(result.mobileError)
        assertEquals(true, result.isValid)
    }

    @Test
    fun `sendOtp is never reached when validate() was never called with a valid mobile`() {
        // Mirrors LoginActivity: sendOtp() is only invoked after validate().isValid is true.
        // This test documents that guard lives in the Activity, not inside sendOtp() itself -
        // sendOtp() has no validation of its own, so calling validate() first is what actually
        // "prevents the API call" for a blank mobile.
        val fakeRepository = FakeRepository()
        val viewModel = LoginViewModel(Application(), fakeRepository)

        val validation = viewModel.validate("")

        assertEquals(false, validation.isValid)
        assertEquals(false, fakeRepository.sendOtpCalled)
    }

    @Test
    fun `sendOtp transitions Idle to Loading then Success`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = LoginViewModel(Application(), fakeRepository)

        assertEquals(LoginUiState.Idle, viewModel.uiState.value)

        viewModel.sendOtp(" 9791222882 ")
        assertEquals(LoginUiState.Loading, viewModel.uiState.value)
        assertEquals("9791222882", fakeRepository.capturedMobile)

        fakeRepository.sendOtpDeferred.complete(
            SendOtpResult.Success(CustomerSendOtpResponse("OTP generated successfully", "307019"))
        )
        advanceUntilIdle()

        assertEquals(LoginUiState.Success("9791222882", "307019"), viewModel.uiState.value)
    }

    @Test
    fun `HTTP 422 error surfaces the backend field message and returns to non-loading state`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = LoginViewModel(Application(), fakeRepository)

        viewModel.sendOtp("9791222882")
        assertEquals(LoginUiState.Loading, viewModel.uiState.value)

        fakeRepository.sendOtpDeferred.complete(
            SendOtpResult.Error(
                reason = ApiErrorReason.VALIDATION_FAILED,
                message = null,
                fieldErrors = mapOf("mobile" to listOf("The mobile field is required."))
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state)
        assertEquals(LoginUiState.Error("The mobile field is required."), state)
    }

    @Test
    fun `network failure surfaces a non-null error message`() = runTest {
        val fakeRepository = FakeRepository()
        val viewModel = LoginViewModel(Application(), fakeRepository)

        viewModel.sendOtp("9791222882")
        fakeRepository.sendOtpDeferred.complete(
            SendOtpResult.Error(reason = ApiErrorReason.NETWORK_UNAVAILABLE, message = "No internet connection.")
        )
        advanceUntilIdle()

        assertEquals(LoginUiState.Error("No internet connection."), viewModel.uiState.value)
    }
}
