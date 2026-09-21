package com.arwe.newproject.ui.otp

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arwe.newproject.data.remote.NetworkModule
import com.arwe.newproject.data.repository.DefaultCustomerAuthRepository
import com.arwe.newproject.session.SessionManager

class OtpVerificationViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val apiService = NetworkModule.getCustomerAuthApiService(application)
        val repository = DefaultCustomerAuthRepository(apiService)
        val sessionManager = SessionManager(application)

        @Suppress("UNCHECKED_CAST")
        return OtpVerificationViewModel(application, repository, sessionManager) as T
    }
}
