package com.arwe.newproject.ui.activeservice

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.arwe.newproject.data.remote.NetworkModule
import com.arwe.newproject.data.repository.DefaultCustomerAuthRepository

class ActiveServiceViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val apiService = NetworkModule.getCustomerAuthApiService(application)
        val repository = DefaultCustomerAuthRepository(apiService)

        @Suppress("UNCHECKED_CAST")
        return ActiveServiceViewModel(application, repository) as T
    }
}
