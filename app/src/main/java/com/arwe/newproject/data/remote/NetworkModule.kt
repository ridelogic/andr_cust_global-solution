package com.arwe.newproject.data.remote

import android.content.Context
import com.arwe.newproject.BuildConfig
import com.arwe.newproject.session.SessionManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    // BuildConfig.API_BASE_URL is the sole source of truth for the backend host - see its own
    // doc in build.gradle.kts. Never hardcode a URL here.
    private val BASE_URL = BuildConfig.API_BASE_URL
    private const val TIMEOUT_SECONDS = 30L

    @Volatile
    private var apiService: CustomerAuthApiService? = null

    fun getCustomerAuthApiService(context: Context): CustomerAuthApiService {
        return apiService ?: synchronized(this) {
            apiService ?: buildApiService(context.applicationContext).also { apiService = it }
        }
    }

    private fun buildApiService(context: Context): CustomerAuthApiService {
        val sessionManager = SessionManager(context)

        // BASIC only logs the request line and response code/length, never the JSON body, so
        // emails/passwords/tokens are never written to Logcat even in debug builds.
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(JsonHeadersInterceptor())
            .addInterceptor(AuthInterceptor(sessionManager))
            .addInterceptor(loggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(CustomerAuthApiService::class.java)
    }
}
