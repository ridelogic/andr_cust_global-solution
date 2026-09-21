package com.arwe.newproject.session

import android.content.Context

class SessionManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun getCustomerName(): String? = prefs.getString(KEY_CUSTOMER_NAME, null)

    fun getAuthToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)

    /**
     * Persists a newly authenticated session - currently written only after a successful
     * POST customer/register (see CustomerAuthRepository/RegisterViewModel). Mirrors the customer
     * fields CustomerResource actually returns (name/mobile/email/customer_code) so screens like
     * Home/Profile can show them without a separate GET customer/profile call. Never the password.
     */
    fun saveSession(token: String, customerCode: String?, name: String?, mobile: String?, email: String?) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putString(KEY_AUTH_TOKEN, token)
            .putString(KEY_CUSTOMER_CODE, customerCode)
            .putString(KEY_CUSTOMER_NAME, name)
            .putString(KEY_CUSTOMER_MOBILE, mobile)
            .putString(KEY_CUSTOMER_EMAIL, email)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "session_prefs"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_CUSTOMER_NAME = "customer_name"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_CUSTOMER_CODE = "customer_code"
        private const val KEY_CUSTOMER_MOBILE = "customer_mobile"
        private const val KEY_CUSTOMER_EMAIL = "customer_email"
    }
}
