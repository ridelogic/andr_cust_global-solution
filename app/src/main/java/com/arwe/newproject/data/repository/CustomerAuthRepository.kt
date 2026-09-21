package com.arwe.newproject.data.repository

import com.arwe.newproject.data.remote.CustomerAuthApiService
import com.arwe.newproject.data.remote.dto.CustomerAddressRequest
import com.arwe.newproject.data.remote.dto.CustomerLoginRequest
import com.arwe.newproject.data.remote.dto.CustomerRegisterRequest
import com.arwe.newproject.data.remote.dto.CustomerRegisterWithOtpRequest
import com.arwe.newproject.data.remote.dto.CustomerSendOtpRequest
import com.arwe.newproject.data.remote.dto.CustomerServiceRequestRequest
import com.arwe.newproject.data.remote.dto.CustomerVerifyOtpRequest
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

interface CustomerAuthRepository {
    suspend fun register(
        name: String,
        mobile: String,
        email: String,
        password: String,
        passwordConfirmation: String
    ): RegisterResult

    suspend fun login(email: String, password: String): RegisterResult

    suspend fun getAddresses(): AddressListResult

    suspend fun createAddress(
        addressType: String,
        addressLine1: String,
        addressLine2: String?,
        landmark: String?,
        city: String,
        state: String,
        postalCode: String
    ): AddressResult

    suspend fun sendOtp(mobile: String): SendOtpResult

    suspend fun verifyOtp(mobile: String, otp: String): VerifyOtpResult

    suspend fun registerWithOtp(mobile: String, name: String, email: String?, registrationToken: String): RegisterResult

    suspend fun getBatteryTypes(): ProductBatteryTypeListResult

    suspend fun createServiceRequest(
        serviceTypeId: Long,
        customerAddressId: Long,
        complaint: String,
        preferredDate: String?,
        preferredTimeFrom: String?,
        preferredTimeTo: String?
    ): ServiceRequestResult
}

/**
 * Talks to POST customer/register - see App\Http\Controllers\Api\Customer\AuthController::register()
 * in the Laravel source (C:\xampp\htdocs\global-services) for the exact contract this mirrors.
 * "alternate_mobile" is never sent - confirmed absent from both that method and
 * App\Actions\Customer\RegisterCustomerAction::handle().
 */
class DefaultCustomerAuthRepository(
    private val apiService: CustomerAuthApiService
) : CustomerAuthRepository {

    override suspend fun register(
        name: String,
        mobile: String,
        email: String,
        password: String,
        passwordConfirmation: String
    ): RegisterResult = withContext(Dispatchers.IO) {
        try {
            val request = CustomerRegisterRequest(
                name = name,
                mobile = mobile,
                email = email,
                password = password,
                passwordConfirmation = passwordConfirmation
            )
            val response = apiService.register(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    RegisterResult.Success(body.token, body.customer)
                } else {
                    RegisterResult.Error(ApiErrorReason.UNKNOWN, null)
                }
            } else {
                val (reason, message, fieldErrors) = classifyHttpError(response)
                RegisterResult.Error(reason, message, fieldErrors)
            }
        } catch (timeout: SocketTimeoutException) {
            RegisterResult.Error(ApiErrorReason.TIMEOUT, null)
        } catch (network: IOException) {
            RegisterResult.Error(ApiErrorReason.NETWORK_UNAVAILABLE, null)
        } catch (unexpected: Exception) {
            RegisterResult.Error(ApiErrorReason.UNKNOWN, null)
        }
    }

    /**
     * Talks to POST customer/login - see AuthController::login() for the exact contract. Only
     * "email" is accepted (not mobile) - see CustomerLoginRequest's doc. A wrong password and a
     * valid-but-unlinked account both render as the SAME HTTP 422 (Laravel's ValidationException,
     * message key "email") - the backend deliberately does not distinguish them, and neither does
     * this method; it is surfaced through the same classifyHttpError() path as every other error.
     */
    override suspend fun login(email: String, password: String): RegisterResult = withContext(Dispatchers.IO) {
        try {
            val request = CustomerLoginRequest(email = email, password = password)
            val response = apiService.login(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    RegisterResult.Success(body.token, body.customer)
                } else {
                    RegisterResult.Error(ApiErrorReason.UNKNOWN, null)
                }
            } else {
                val (reason, message, fieldErrors) = classifyHttpError(response)
                RegisterResult.Error(reason, message, fieldErrors)
            }
        } catch (timeout: SocketTimeoutException) {
            RegisterResult.Error(ApiErrorReason.TIMEOUT, null)
        } catch (network: IOException) {
            RegisterResult.Error(ApiErrorReason.NETWORK_UNAVAILABLE, null)
        } catch (unexpected: Exception) {
            RegisterResult.Error(ApiErrorReason.UNKNOWN, null)
        }
    }

    /** Talks to GET customer/addresses - see AddressController::index() for the exact contract. */
    override suspend fun getAddresses(): AddressListResult = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAddresses()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    AddressListResult.Success(body)
                } else {
                    AddressListResult.Error(ApiErrorReason.UNKNOWN, null)
                }
            } else {
                val (reason, message, fieldErrors) = classifyHttpError(response)
                AddressListResult.Error(reason, message, fieldErrors)
            }
        } catch (timeout: SocketTimeoutException) {
            AddressListResult.Error(ApiErrorReason.TIMEOUT, null)
        } catch (network: IOException) {
            AddressListResult.Error(ApiErrorReason.NETWORK_UNAVAILABLE, null)
        } catch (unexpected: Exception) {
            AddressListResult.Error(ApiErrorReason.UNKNOWN, null)
        }
    }

    /**
     * Talks to POST customer/addresses - see AddressController::store()/validated() for the
     * exact contract. "is_default" is always sent as false - there is no "set as default" UI yet,
     * and the backend requires the key to be present (see CustomerAddressRequest's doc).
     */
    override suspend fun createAddress(
        addressType: String,
        addressLine1: String,
        addressLine2: String?,
        landmark: String?,
        city: String,
        state: String,
        postalCode: String
    ): AddressResult = withContext(Dispatchers.IO) {
        try {
            val request = CustomerAddressRequest(
                addressType = addressType,
                addressLine1 = addressLine1,
                addressLine2 = addressLine2,
                landmark = landmark,
                city = city,
                state = state,
                postalCode = postalCode,
                isDefault = false
            )
            val response = apiService.createAddress(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    AddressResult.Success(body)
                } else {
                    AddressResult.Error(ApiErrorReason.UNKNOWN, null)
                }
            } else {
                val (reason, message, fieldErrors) = classifyHttpError(response)
                AddressResult.Error(reason, message, fieldErrors)
            }
        } catch (timeout: SocketTimeoutException) {
            AddressResult.Error(ApiErrorReason.TIMEOUT, null)
        } catch (network: IOException) {
            AddressResult.Error(ApiErrorReason.NETWORK_UNAVAILABLE, null)
        } catch (unexpected: Exception) {
            AddressResult.Error(ApiErrorReason.UNKNOWN, null)
        }
    }

    /**
     * Talks to POST customer/send-otp - see AuthController::sendOtp() for the exact contract.
     * Public endpoint, no Authorization header needed (AuthInterceptor already only attaches one
     * when a token exists, and none exists yet at this point in the flow).
     */
    override suspend fun sendOtp(mobile: String): SendOtpResult = withContext(Dispatchers.IO) {
        try {
            val response = apiService.sendOtp(CustomerSendOtpRequest(mobile))
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    SendOtpResult.Success(body)
                } else {
                    SendOtpResult.Error(ApiErrorReason.UNKNOWN, null)
                }
            } else {
                val (reason, message, fieldErrors) = classifyHttpError(response)
                SendOtpResult.Error(reason, message, fieldErrors)
            }
        } catch (timeout: SocketTimeoutException) {
            SendOtpResult.Error(ApiErrorReason.TIMEOUT, null)
        } catch (network: IOException) {
            SendOtpResult.Error(ApiErrorReason.NETWORK_UNAVAILABLE, null)
        } catch (unexpected: Exception) {
            SendOtpResult.Error(ApiErrorReason.UNKNOWN, null)
        }
    }

    /**
     * Talks to POST customer/verify-otp - see AuthController::verifyOtp() for the exact contract.
     * Disambiguates the two possible 200 shapes here, via explicit null-checks only (no unsafe
     * casts) - see CustomerVerifyOtpResponse's doc for why both shapes share one DTO.
     */
    override suspend fun verifyOtp(mobile: String, otp: String): VerifyOtpResult = withContext(Dispatchers.IO) {
        try {
            val response = apiService.verifyOtp(CustomerVerifyOtpRequest(mobile, otp))
            if (response.isSuccessful) {
                val body = response.body()
                val registrationToken = body?.registrationToken
                val token = body?.token
                val customer = body?.customer
                when {
                    body == null -> VerifyOtpResult.Error(ApiErrorReason.UNKNOWN, null)
                    body.requiresRegistration == true && body.mobile != null && registrationToken != null ->
                        VerifyOtpResult.RegistrationRequired(body.mobile, registrationToken)
                    token != null && customer != null -> VerifyOtpResult.ExistingCustomer(token, customer)
                    else -> VerifyOtpResult.Error(ApiErrorReason.UNKNOWN, null)
                }
            } else {
                val (reason, message, fieldErrors) = classifyHttpError(response)
                VerifyOtpResult.Error(reason, message, fieldErrors)
            }
        } catch (timeout: SocketTimeoutException) {
            VerifyOtpResult.Error(ApiErrorReason.TIMEOUT, null)
        } catch (network: IOException) {
            VerifyOtpResult.Error(ApiErrorReason.NETWORK_UNAVAILABLE, null)
        } catch (unexpected: Exception) {
            VerifyOtpResult.Error(ApiErrorReason.UNKNOWN, null)
        }
    }

    /**
     * Talks to POST customer/register-with-otp - see AuthController::registerWithOtp() for the
     * exact contract. Reuses RegisterResult (identical {token, customer} success shape to
     * register()/login()) rather than a new result type.
     */
    override suspend fun registerWithOtp(
        mobile: String,
        name: String,
        email: String?,
        registrationToken: String
    ): RegisterResult = withContext(Dispatchers.IO) {
        try {
            val request = CustomerRegisterWithOtpRequest(
                mobile = mobile,
                name = name,
                email = email,
                registrationToken = registrationToken
            )
            val response = apiService.registerWithOtp(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    RegisterResult.Success(body.token, body.customer)
                } else {
                    RegisterResult.Error(ApiErrorReason.UNKNOWN, null)
                }
            } else {
                val (reason, message, fieldErrors) = classifyHttpError(response)
                RegisterResult.Error(reason, message, fieldErrors)
            }
        } catch (timeout: SocketTimeoutException) {
            RegisterResult.Error(ApiErrorReason.TIMEOUT, null)
        } catch (network: IOException) {
            RegisterResult.Error(ApiErrorReason.NETWORK_UNAVAILABLE, null)
        } catch (unexpected: Exception) {
            RegisterResult.Error(ApiErrorReason.UNKNOWN, null)
        }
    }

    /** Talks to GET customer/products/battery-types - see ProductController::batteryTypes(). */
    override suspend fun getBatteryTypes(): ProductBatteryTypeListResult = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getBatteryTypes()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    ProductBatteryTypeListResult.Success(body)
                } else {
                    ProductBatteryTypeListResult.Error(ApiErrorReason.UNKNOWN, null)
                }
            } else {
                val (reason, message, fieldErrors) = classifyHttpError(response)
                ProductBatteryTypeListResult.Error(reason, message, fieldErrors)
            }
        } catch (timeout: SocketTimeoutException) {
            ProductBatteryTypeListResult.Error(ApiErrorReason.TIMEOUT, null)
        } catch (network: IOException) {
            ProductBatteryTypeListResult.Error(ApiErrorReason.NETWORK_UNAVAILABLE, null)
        } catch (unexpected: Exception) {
            ProductBatteryTypeListResult.Error(ApiErrorReason.UNKNOWN, null)
        }
    }

    /**
     * Talks to POST customer/service-requests - see ServiceRequestController::store() for the
     * exact contract. customer_product_id/product_model_id/serial_number/purchase_date are never
     * sent - see CustomerServiceRequestRequest's doc.
     */
    override suspend fun createServiceRequest(
        serviceTypeId: Long,
        customerAddressId: Long,
        complaint: String,
        preferredDate: String?,
        preferredTimeFrom: String?,
        preferredTimeTo: String?
    ): ServiceRequestResult = withContext(Dispatchers.IO) {
        try {
            val request = CustomerServiceRequestRequest(
                serviceTypeId = serviceTypeId,
                customerAddressId = customerAddressId,
                complaint = complaint,
                preferredDate = preferredDate,
                preferredTimeFrom = preferredTimeFrom,
                preferredTimeTo = preferredTimeTo
            )
            val response = apiService.createServiceRequest(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    ServiceRequestResult.Success(body)
                } else {
                    ServiceRequestResult.Error(ApiErrorReason.UNKNOWN, null)
                }
            } else {
                val (reason, message, fieldErrors) = classifyHttpError(response)
                ServiceRequestResult.Error(reason, message, fieldErrors)
            }
        } catch (timeout: SocketTimeoutException) {
            ServiceRequestResult.Error(ApiErrorReason.TIMEOUT, null)
        } catch (network: IOException) {
            ServiceRequestResult.Error(ApiErrorReason.NETWORK_UNAVAILABLE, null)
        } catch (unexpected: Exception) {
            ServiceRequestResult.Error(ApiErrorReason.UNKNOWN, null)
        }
    }

    private fun classifyHttpError(
        response: Response<*>
    ): Triple<ApiErrorReason, String?, Map<String, List<String>>?> {
        val rawBody = response.errorBody()?.string()
        val envelope = parseErrorBody(rawBody)
        return Triple(reasonForCode(response.code()), envelope?.message, envelope?.errors)
    }

    private fun reasonForCode(code: Int): ApiErrorReason = when (code) {
        401 -> ApiErrorReason.UNAUTHORIZED
        403 -> ApiErrorReason.FORBIDDEN
        404 -> ApiErrorReason.NOT_FOUND
        422 -> ApiErrorReason.VALIDATION_FAILED
        429 -> ApiErrorReason.TOO_MANY_ATTEMPTS
        in 500..599 -> ApiErrorReason.SERVER_ERROR
        else -> ApiErrorReason.UNKNOWN
    }

    /**
     * Reads Laravel's error envelope, which takes one of two confirmed shapes for this endpoint
     * (see AuthController::register()/RegisterCustomerAction::handle()/bootstrap/app.php):
     * {"message": "...", "errors": {"field": ["msg", ...]}} for a standard validation failure, or
     * {"message": "..."} only for the RegisterCustomerAction business-rule rejection. "errors" is
     * simply absent/null in the second case - never assumed present.
     */
    private fun parseErrorBody(rawBody: String?): ApiErrorEnvelope? {
        if (rawBody.isNullOrBlank()) return null
        return runCatching { Gson().fromJson(rawBody, ApiErrorEnvelope::class.java) }.getOrNull()
    }

    private data class ApiErrorEnvelope(
        val message: String?,
        val errors: Map<String, List<String>>?
    )
}
