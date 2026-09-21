package com.arwe.newproject.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Declares on every request that a JSON response is wanted. Without this, Laravel's
 * Request::expectsJson() (which inspects the Accept header) renders a validation/auth failure as
 * an HTML redirect instead of a JSON error body, since Retrofit's Gson converter never sets
 * Accept itself - only Content-Type. Confirmed against this exact backend by the sibling
 * Technician app (see its own JsonHeadersInterceptor).
 */
class JsonHeadersInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("Accept", "application/json")
            .build()
        return chain.proceed(request)
    }
}
