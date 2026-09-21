package com.arwe.newproject.ui.booking

sealed class ProductListUiState {
    object Loading : ProductListUiState()
    data class Loaded(val products: List<Product>) : ProductListUiState()
    data class Error(val message: String) : ProductListUiState()
}
