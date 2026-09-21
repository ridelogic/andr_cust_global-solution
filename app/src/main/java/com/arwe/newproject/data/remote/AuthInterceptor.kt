package com.arwe.newproject.data.remote

import com.arwe.newproject.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches "Authorization: Bearer <token>" using the token persisted in SessionManager, for any
 * future authenticated "customer" endpoint. Skipped for register/login themselves, which are
 * public (no auth:sanctum middleware - see routes/api.php).
 */
class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val path = request.url.encodedPath
        if (path.endsWith(REGISTER_PATH) || path.endsWith(LOGIN_PATH)) {
            return chain.proceed(request)
        }

        val token = sessionManager.getAuthToken()
        val authorizedRequest = if (!token.isNullOrBlank()) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }

        return chain.proceed(authorizedRequest)
    }

    private companion object {
        const val REGISTER_PATH = "customer/register"
        const val LOGIN_PATH = "customer/login"
    }
}
