package com.arwe.newproject.ui.otp

import android.app.Application
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.arwe.newproject.data.remote.dto.CustomerDto
import com.arwe.newproject.data.remote.dto.CustomerRegisterWithOtpRequest
import com.arwe.newproject.data.repository.AddressListResult
import com.arwe.newproject.data.repository.AddressResult
import com.arwe.newproject.data.repository.ApiErrorReason
import com.arwe.newproject.data.repository.CustomerAuthRepository
import com.arwe.newproject.data.repository.ProductBatteryTypeListResult
import com.arwe.newproject.data.repository.RegisterResult
import com.arwe.newproject.data.repository.ServiceRequestListResult
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerOtpRegistrationViewModelTest {

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
        var registerWithOtpCalled = false
        val registerWithOtpDeferred = CompletableDeferred<RegisterResult>()

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
        ): RegisterResult {
            registerWithOtpCalled = true
            return registerWithOtpDeferred.await()
        }

        override suspend fun getBatteryTypes(): ProductBatteryTypeListResult =
            throw UnsupportedOperationException("not used by this test")

        override suspend fun createServiceRequest(
            serviceTypeId: Long, customerAddressId: Long, complaint: String,
            preferredDate: String?, preferredTimeFrom: String?, preferredTimeTo: String?
        ): ServiceRequestResult = throw UnsupportedOperationException("not used by this test")

        override suspend fun getServiceRequests(): ServiceRequestListResult =
            throw UnsupportedOperationException("not used by this test")
    }

    @Test
    fun `blank name fails validation`() {
        val fakeApp = FakeApplication()
        val viewModel = CustomerOtpRegistrationViewModel(fakeApp, FakeRepository(), SessionManager(fakeApp))

        val result = viewModel.validate("   ", "")

        assertEquals(OtpRegistrationFieldError.REQUIRED, result.nameError)
        assertFalse(result.isValid)
    }

    @Test
    fun `missing registration token prevents the API call`() {
        val fakeApp = FakeApplication()
        val fakeRepository = FakeRepository()
        val viewModel = CustomerOtpRegistrationViewModel(fakeApp, fakeRepository, SessionManager(fakeApp))

        viewModel.register(mobile = "9791222882", name = "Test Customer", email = "", registrationToken = "")

        assertFalse(fakeRepository.registerWithOtpCalled)
        assertEquals(CustomerOtpRegistrationUiState.MissingContext, viewModel.uiState.value)
    }

    @Test
    fun `register transitions Idle to Loading then Success and saves the session`() = runTest {
        val fakeApp = FakeApplication()
        val fakeRepository = FakeRepository()
        val sessionManager = SessionManager(fakeApp)
        val viewModel = CustomerOtpRegistrationViewModel(fakeApp, fakeRepository, sessionManager)

        assertEquals(CustomerOtpRegistrationUiState.Idle, viewModel.uiState.value)

        viewModel.register(
            mobile = "9791222882", name = "Test Customer", email = "testcustomer2026@gmail.com",
            registrationToken = "reg-token-abc"
        )
        assertEquals(CustomerOtpRegistrationUiState.Loading, viewModel.uiState.value)

        val customer = CustomerDto(
            id = 8, customerCode = "CUST-0008", name = "Test Customer", mobile = "9791222882",
            alternateMobile = null, email = "testcustomer2026@gmail.com"
        )
        fakeRepository.registerWithOtpDeferred.complete(RegisterResult.Success("plain-text-token", customer))
        advanceUntilIdle()

        // This is the state OtpVerificationActivity's Success branch turns into Home navigation.
        assertEquals(
            CustomerOtpRegistrationUiState.Success("plain-text-token", customer),
            viewModel.uiState.value
        )
        assertTrue(sessionManager.isLoggedIn())
        assertEquals("plain-text-token", sessionManager.getAuthToken())
    }

    @Test
    fun `HTTP 422 duplicate account surfaces the backend message`() = runTest {
        val fakeApp = FakeApplication()
        val fakeRepository = FakeRepository()
        val viewModel = CustomerOtpRegistrationViewModel(fakeApp, fakeRepository, SessionManager(fakeApp))

        viewModel.register("9791222882", "Test Customer", "", "reg-token-abc")
        fakeRepository.registerWithOtpDeferred.complete(
            RegisterResult.Error(
                reason = ApiErrorReason.VALIDATION_FAILED,
                message = "An account already exists for this mobile number."
            )
        )
        advanceUntilIdle()

        assertEquals(
            CustomerOtpRegistrationUiState.Error("An account already exists for this mobile number."),
            viewModel.uiState.value
        )
    }

    @Test
    fun `network failure surfaces a non-null error state`() = runTest {
        val fakeApp = FakeApplication()
        val fakeRepository = FakeRepository()
        val viewModel = CustomerOtpRegistrationViewModel(fakeApp, fakeRepository, SessionManager(fakeApp))

        viewModel.register("9791222882", "Test Customer", "", "reg-token-abc")
        fakeRepository.registerWithOtpDeferred.complete(
            RegisterResult.Error(reason = ApiErrorReason.NETWORK_UNAVAILABLE, message = "No internet connection.")
        )
        advanceUntilIdle()

        assertEquals(CustomerOtpRegistrationUiState.Error("No internet connection."), viewModel.uiState.value)
    }

    @Test
    fun `CustomerRegisterWithOtpRequest has no password field`() {
        val fieldNames = CustomerRegisterWithOtpRequest::class.java.declaredFields.map { it.name.lowercase() }

        assertFalse(fieldNames.any { it.contains("password") })
        assertFalse(fieldNames.any { it.contains("otp") })
    }
}
