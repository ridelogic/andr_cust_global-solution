package com.arwe.newproject.ui.register

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arwe.newproject.data.remote.NetworkModule
import com.arwe.newproject.data.repository.DefaultCustomerAuthRepository
import com.arwe.newproject.session.SessionManager

class RegisterViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val apiService = NetworkModule.getCustomerAuthApiService(application)
        val repository = DefaultCustomerAuthRepository(apiService)
        val sessionManager = SessionManager(application)

        @Suppress("UNCHECKED_CAST")
        return RegisterViewModel(application, repository, sessionManager) as T
    }
}
