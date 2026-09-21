package com.arwe.newproject.ui.booking

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arwe.newproject.R
import com.arwe.newproject.data.repository.ApiErrorReason
import com.arwe.newproject.data.repository.CustomerAuthRepository
import com.arwe.newproject.data.repository.ProductBatteryTypeListResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProductSelectionViewModel(
    application: Application,
    private val repository: CustomerAuthRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ProductListUiState>(ProductListUiState.Loading)
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    /**
     * BATTERY is the only category backed by a real endpoint so far (GET
     * customer/products/battery-types) - every other category keeps using the existing static
     * ProductCatalog data, loading synchronously with no network call, exactly as before this
     * screen was integrated.
     */
    fun loadProducts(category: ServiceCategory) {
        if (category != ServiceCategory.BATTERY) {
            _uiState.value = ProductListUiState.Loaded(ProductCatalog.productsFor(category))
            return
        }

        viewModelScope.launch {
            _uiState.value = ProductListUiState.Loading
            when (val result = repository.getBatteryTypes()) {
                is ProductBatteryTypeListResult.Success -> {
                    val products = result.batteryTypes.mapNotNull { ProductCatalog.batteryProductForCode(it.code) }
                    _uiState.value = ProductListUiState.Loaded(products)
                }

                is ProductBatteryTypeListResult.Error -> {
                    _uiState.value = ProductListUiState.Error(messageFor(result))
                }
            }
        }
    }

    private fun messageFor(error: ProductBatteryTypeListResult.Error): String {
        val app = getApplication<Application>()
        return error.message ?: app.getString(fallbackProductListMessageResFor(error.reason))
    }
}

internal fun fallbackProductListMessageResFor(reason: ApiErrorReason): Int = when (reason) {
    ApiErrorReason.UNAUTHORIZED -> R.string.product_list_error_unauthorized
    ApiErrorReason.TOO_MANY_ATTEMPTS -> R.string.add_address_error_too_many_attempts
    ApiErrorReason.SERVER_ERROR -> R.string.add_address_error_server
    ApiErrorReason.NETWORK_UNAVAILABLE -> R.string.add_address_error_network_unavailable
    ApiErrorReason.TIMEOUT -> R.string.add_address_error_timeout
    ApiErrorReason.FORBIDDEN,
    ApiErrorReason.NOT_FOUND,
    ApiErrorReason.VALIDATION_FAILED,
    ApiErrorReason.UNKNOWN -> R.string.product_list_error_generic
}
