package com.arwe.newproject.ui.login

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arwe.newproject.data.remote.NetworkModule
import com.arwe.newproject.data.repository.DefaultCustomerAuthRepository

class LoginViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val apiService = NetworkModule.getCustomerAuthApiService(application)
        val repository = DefaultCustomerAuthRepository(apiService)

        @Suppress("UNCHECKED_CAST")
        return LoginViewModel(application, repository) as T
    }
}
