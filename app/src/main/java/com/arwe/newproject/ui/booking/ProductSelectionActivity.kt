package com.arwe.newproject.ui.booking

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.arwe.newproject.R
import com.arwe.newproject.databinding.ActivityProductSelectionBinding
import com.arwe.newproject.databinding.ItemProductBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ProductSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductSelectionBinding
    private lateinit var category: ServiceCategory
    private val viewModel: ProductSelectionViewModel by viewModels { ProductSelectionViewModelFactory(application) }

    private var selectedProduct: Product? = null
    private val productItems = mutableMapOf<Product, ItemProductBinding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        category = resolveCategory(intent.getStringExtra(ServiceCategoryActivity.EXTRA_SELECTED_CATEGORY))

        binding.backButton.setOnClickListener { finish() }

        binding.continueButton.setOnClickListener {
            val product = selectedProduct ?: return@setOnClickListener
            val intent = Intent(this, ProblemSelectionActivity::class.java)
                .putExtra(ServiceCategoryActivity.EXTRA_SELECTED_CATEGORY, category.id)
                .putExtra(EXTRA_SELECTED_PRODUCT, product.id)
            startActivity(intent)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }

        viewModel.loadProducts(category)
    }

    private fun render(state: ProductListUiState) {
        when (state) {
            is ProductListUiState.Loading -> {
                binding.productListProgress.visibility = View.VISIBLE
                binding.productContainer.visibility = View.GONE
            }

            is ProductListUiState.Loaded -> {
                binding.productListProgress.visibility = View.GONE
                binding.productContainer.visibility = View.VISIBLE
                bindProductList(state.products)
            }

            is ProductListUiState.Error -> {
                binding.productListProgress.visibility = View.GONE
                binding.productContainer.visibility = View.VISIBLE
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun bindProductList(products: List<Product>) {
        binding.productContainer.removeAllViews()
        productItems.clear()
        selectedProduct = null
        binding.continueButton.isEnabled = false

        products.forEachIndexed { index, product ->
            addProductItem(product, index > 0)
        }
    }

    private fun addProductItem(product: Product, addTopSpacing: Boolean) {
        val item = ItemProductBinding.inflate(layoutInflater, binding.productContainer, false)
        item.productIcon.setImageResource(product.iconRes)
        item.productTitle.setText(product.titleRes)
        item.productDescription.setText(product.descriptionRes)
        item.root.contentDescription = getString(product.titleRes) + ". " + getString(product.descriptionRes)
        item.root.setOnClickListener { selectProduct(product) }

        if (addTopSpacing) {
            val params = item.root.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = resources.getDimensionPixelSize(R.dimen.home_quick_card_spacing)
            item.root.layoutParams = params
        }

        binding.productContainer.addView(item.root)
        productItems[product] = item
    }

    private fun selectProduct(product: Product) {
        selectedProduct = product
        productItems.forEach { (candidate, item) -> item.root.isSelected = candidate == product }
        binding.continueButton.isEnabled = true
    }

    private fun resolveCategory(id: String?): ServiceCategory =
        ServiceCategory.values().firstOrNull { it.id == id } ?: ServiceCategory.OTHER

    companion object {
        const val EXTRA_SELECTED_PRODUCT = "extra_selected_product"
    }
}
