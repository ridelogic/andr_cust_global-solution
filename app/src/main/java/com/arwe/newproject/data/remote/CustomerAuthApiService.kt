package com.arwe.newproject.data.remote

import com.arwe.newproject.data.remote.dto.CustomerAddressRequest
import com.arwe.newproject.data.remote.dto.CustomerAddressResponse
import com.arwe.newproject.data.remote.dto.CustomerLoginRequest
import com.arwe.newproject.data.remote.dto.CustomerRegisterRequest
import com.arwe.newproject.data.remote.dto.CustomerRegisterResponse
import com.arwe.newproject.data.remote.dto.CustomerRegisterWithOtpRequest
import com.arwe.newproject.data.remote.dto.CustomerSendOtpRequest
import com.arwe.newproject.data.remote.dto.CustomerSendOtpResponse
import com.arwe.newproject.data.remote.dto.CustomerServiceRequestRequest
import com.arwe.newproject.data.remote.dto.CustomerServiceRequestResponse
import com.arwe.newproject.data.remote.dto.CustomerVerifyOtpRequest
import com.arwe.newproject.data.remote.dto.CustomerVerifyOtpResponse
import com.arwe.newproject.data.remote.dto.ProductBatteryTypeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface CustomerAuthApiService {

    // routes/api.php: Route::prefix('customer')->... ->
    // Route::post('register', [CustomerAuthController::class, 'register']) - public, no
    // auth:sanctum middleware. Resolves to POST {API_BASE_URL}customer/register.
    @POST("customer/register")
    suspend fun register(@Body request: CustomerRegisterRequest): Response<CustomerRegisterResponse>

    // routes/api.php: Route::post('login', [CustomerAuthController::class, 'login']) - public,
    // same customer prefix group as register. Resolves to POST {API_BASE_URL}customer/login.
    // Success has the exact same {token, customer} shape as register (200, not 201) - see
    // AuthController::login() - so CustomerRegisterResponse is reused rather than duplicated.
    @POST("customer/login")
    suspend fun login(@Body request: CustomerLoginRequest): Response<CustomerRegisterResponse>

    // routes/api.php: Route::prefix('addresses')->... -> Route::get('/', [AddressController::class,
    // 'index']) - authenticated (auth:sanctum + customer.active), AuthInterceptor attaches the
    // Bearer token automatically. Plain JSON array, no wrapper/pagination - see
    // AddressController::index().
    @GET("customer/addresses")
    suspend fun getAddresses(): Response<List<CustomerAddressResponse>>

    // routes/api.php: Route::post('/', [AddressController::class, 'store']) - same auth as above.
    // Returns HTTP 200 (not 201 - store() sets no explicit status) with a single AddressResource.
    @POST("customer/addresses")
    suspend fun createAddress(@Body request: CustomerAddressRequest): Response<CustomerAddressResponse>

    // routes/api.php: Route::post('send-otp', [CustomerAuthController::class, 'sendOtp']) - public,
    // same customer prefix group as register/login, no auth:sanctum. See
    // AuthController::sendOtp() - the response's "otp" field exists only because this endpoint is
    // in its development/testing phase; never logged or persisted on this side.
    @POST("customer/send-otp")
    suspend fun sendOtp(@Body request: CustomerSendOtpRequest): Response<CustomerSendOtpResponse>

    // routes/api.php: Route::post('verify-otp', [CustomerAuthController::class, 'verifyOtp']) -
    // public, same customer prefix group as send-otp. See AuthController::verifyOtp() - the 200
    // response is one of two shapes (existing-customer token, or requires_registration); see
    // CustomerVerifyOtpResponse's doc.
    @POST("customer/verify-otp")
    suspend fun verifyOtp(@Body request: CustomerVerifyOtpRequest): Response<CustomerVerifyOtpResponse>

    // routes/api.php: Route::post('register-with-otp', [CustomerAuthController::class,
    // 'registerWithOtp']) - public, same customer prefix group as send-otp/verify-otp. See
    // AuthController::registerWithOtp() - HTTP 201 with the exact same {token, customer} shape as
    // register()/login(), so CustomerRegisterResponse is reused rather than duplicated.
    @POST("customer/register-with-otp")
    suspend fun registerWithOtp(@Body request: CustomerRegisterWithOtpRequest): Response<CustomerRegisterResponse>

    // routes/api.php: Route::prefix('products')->... -> Route::get('battery-types',
    // [CustomerProductController::class, 'batteryTypes']) - authenticated (auth:sanctum +
    // customer.active). Read-only catalog lookup, NOT the customer's own registered products (see
    // ProductController::batteryTypes()'s doc for the "Battery Type Scope Lock" distinction from
    // GET customer/products, which is a different, customer-owned-batteries endpoint this screen
    // does not use).
    @GET("customer/products/battery-types")
    suspend fun getBatteryTypes(): Response<List<ProductBatteryTypeResponse>>

    // routes/api.php: Route::prefix('service-requests')->... -> Route::post('/',
    // [CustomerServiceRequestController::class, 'store']) - authenticated (auth:sanctum +
    // customer.active). See ServiceRequestController::store() - returns a
    // ServiceRequestDetailResource (only id/ticket_number are modeled - see
    // CustomerServiceRequestResponse's doc).
    @POST("customer/service-requests")
    suspend fun createServiceRequest(
        @Body request: CustomerServiceRequestRequest
    ): Response<CustomerServiceRequestResponse>
}
