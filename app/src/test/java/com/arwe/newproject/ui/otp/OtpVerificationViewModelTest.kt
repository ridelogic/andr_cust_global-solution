package com.arwe.newproject.ui.otp

import android.app.Application
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.arwe.newproject.data.remote.dto.CustomerDto
import com.arwe.newproject.data.repository.AddressListResult
import com.arwe.newproject.data.repository.AddressResult
import com.arwe.newproject.data.repository.ApiErrorReason
import com.arwe.newproject.data.repository.CustomerAuthRepository
import com.arwe.newproject.data.repository.ProductBatteryTypeListResult
import com.arwe.newproject.data.repository.RegisterResult
import com.arwe.newproject.data.repository.ServiceRequestResult
import com.arwe.newproject.data.repository.SendOtpResult
import com.arwe.newproject.data.repository.VerifyOtpResult
import com.arwe.newproject.session.SessionManager
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OtpVerificationViewModelTest {

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

    /** In-memory SharedPreferences so SessionManager.saveSession()'s real code path can run and
     * be observed without touching SessionManager itself or Robolectric. */
    private class FakeSharedPreferences : SharedPreferences {
        val values = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = values
        override fun getString(key: String?, defValue: String?) = values[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String?, defValue: Int) = values[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long) = values[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float) = values[key] as? Float ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean) = values[key] as? Boolean ?: defValue
        override fun contains(key: String?) = values.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor()
        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit

        inner class FakeEditor : SharedPreferences.Editor {
            private val pending = mutableMapOf<String, Any?>()

            override fun putString(key: String?, value: String?) = apply { pending[key!!] = value }
            override fun putStringSet(key: String?, values: MutableSet<String>?) = apply { pending[key!!] = values }
            override fun putInt(key: String?, value: Int) = apply { pending[key!!] = value }
            override fun putLong(key: String?, value: Long) = apply { pending[key!!] = value }
            override fun putFloat(key: String?, value: Float) = apply { pending[key!!] = value }
            override fun putBoolean(key: String?, value: Boolean) = apply { pending[key!!] = value }
            override fun remove(key: String?) = apply { pending[key!!] = null }
            override fun clear() = apply { values.clear() }
            override fun commit(): Boolean { apply(); return true }
            override fun apply() { values.putAll(pending) }
        }
    }

    private class FakeApplication : Application() {
        val prefs = FakeSharedPreferences()
        override fun getApplicationContext() = this
        override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = prefs
    }

    private class FakeRepository : CustomerAuthRepository {
        var verifyOtpCalled = false
        var capturedMobile: String? = null
        var capturedOtp: String? = null
        val verifyOtpDeferred = CompletableDeferred<VerifyOtpResult>()

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

        override suspend fun verifyOtp(mobile: String, otp: String): VerifyOtpResult {
            verifyOtpCalled = true
            capturedMobile = mobile
            capturedOtp = otp
            return verifyOtpDeferred.await()
        }

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
    fun `blank OTP fails validation and never reaches the repository`() {
        val fakeApp = FakeApplication()
        val fakeRepository = FakeRepository()
        val viewModel = OtpVerificationViewModel(fakeApp, fakeRepository, SessionManager(fakeApp))

        val validation = viewModel.validate("   ")

        assertEquals(OtpFieldError.REQUIRED, validation.otpError)
        assertFalse(validation.isValid)
        assertFalse(fakeRepository.verifyOtpCalled)
    }

    @Test
    fun `verifyOtp transitions Idle to Loading then ExistingCustomerSuccess and saves the session`() = runTest {
        val fakeApp = FakeApplication()
        val fakeRepository = FakeRepository()
        val sessionManager = SessionManager(fakeApp)
        val viewModel = OtpVerificationViewModel(fakeApp, fakeRepository, sessionManager)

        assertEquals(OtpVerificationUiState.Idle, viewModel.uiState.value)

        viewModel.verifyOtp("9791222882", "307019")
        assertEquals(OtpVerificationUiState.Loading, viewModel.uiState.value)

        val customer = CustomerDto(
            id = 7, customerCode = "CUST-0007", name = "Vikram", mobile = "9791222882",
            alternateMobile = null, email = null
        )
        fakeRepository.verifyOtpDeferred.complete(VerifyOtpResult.ExistingCustomer("plain-text-token", customer))
        advanceUntilIdle()

        assertEquals(
            OtpVerificationUiState.ExistingCustomerSuccess("plain-text-token", customer),
            viewModel.uiState.value
        )
        assertTrue(sessionManager.isLoggedIn())
        assertEquals("plain-text-token", sessionManager.getAuthToken())
    }

    @Test
    fun `verifyOtp with requires_registration does not save a session`() = runTest {
        val fakeApp = FakeApplication()
        val fakeRepository = FakeRepository()
        val sessionManager = SessionManager(fakeApp)
        val viewModel = OtpVerificationViewModel(fakeApp, fakeRepository, sessionManager)

        viewModel.verifyOtp("9791222882", "307019")
        fakeRepository.verifyOtpDeferred.complete(
            VerifyOtpResult.RegistrationRequired("9791222882", "reg-token-abc")
        )
        advanceUntilIdle()

        assertEquals(
            OtpVerificationUiState.RegistrationRequired("9791222882", "reg-token-abc"),
            viewModel.uiState.value
        )
        assertFalse(sessionManager.isLoggedIn())
        assertNull(sessionManager.getAuthToken())
    }

    @Test
    fun `HTTP 422 invalid OTP surfaces the backend message and does not save a session`() = runTest {
        val fakeApp = FakeApplication()
        val fakeRepository = FakeRepository()
        val sessionManager = SessionManager(fakeApp)
        val viewModel = OtpVerificationViewModel(fakeApp, fakeRepository, sessionManager)

        viewModel.verifyOtp("9791222882", "000000")
        fakeRepository.verifyOtpDeferred.complete(
            VerifyOtpResult.Error(reason = ApiErrorReason.VALIDATION_FAILED, message = "Invalid OTP.")
        )
        advanceUntilIdle()

        assertEquals(OtpVerificationUiState.Error("Invalid OTP."), viewModel.uiState.value)
        assertFalse(sessionManager.isLoggedIn())
    }

    @Test
    fun `HTTP 422 expired OTP surfaces the backend message`() = runTest {
        val fakeApp = FakeApplication()
        val fakeRepository = FakeRepository()
        val viewModel = OtpVerificationViewModel(fakeApp, fakeRepository, SessionManager(fakeApp))

        viewModel.verifyOtp("9791222882", "307019")
        fakeRepository.verifyOtpDeferred.complete(
            VerifyOtpResult.Error(
                reason = ApiErrorReason.VALIDATION_FAILED,
                message = "OTP has expired. Please request a new one."
            )
        )
        advanceUntilIdle()

        assertEquals(
            OtpVerificationUiState.Error("OTP has expired. Please request a new one."),
            viewModel.uiState.value
        )
    }

    @Test
    fun `network failure surfaces a non-null error state`() = runTest {
        val fakeApp = FakeApplication()
        val fakeRepository = FakeRepository()
        val viewModel = OtpVerificationViewModel(fakeApp, fakeRepository, SessionManager(fakeApp))

        viewModel.verifyOtp("9791222882", "307019")
        fakeRepository.verifyOtpDeferred.complete(
            VerifyOtpResult.Error(reason = ApiErrorReason.NETWORK_UNAVAILABLE, message = "No internet connection.")
        )
        advanceUntilIdle()

        assertEquals(OtpVerificationUiState.Error("No internet connection."), viewModel.uiState.value)
    }
}
