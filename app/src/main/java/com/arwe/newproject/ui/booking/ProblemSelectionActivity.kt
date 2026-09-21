package com.arwe.newproject.ui.booking

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.arwe.newproject.R
import com.arwe.newproject.databinding.ActivityProblemSelectionBinding
import com.arwe.newproject.databinding.ItemProblemBinding

class ProblemSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProblemSelectionBinding
    private lateinit var category: ServiceCategory
    private lateinit var product: Product
    private var selectedProblem: Problem? = null
    private val problemItems = mutableMapOf<Problem, ItemProblemBinding>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProblemSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        category = resolveCategory(intent.getStringExtra(ServiceCategoryActivity.EXTRA_SELECTED_CATEGORY))
        product = resolveProduct(category, intent.getStringExtra(ProductSelectionActivity.EXTRA_SELECTED_PRODUCT))

        binding.backButton.setOnClickListener { finish() }

        ProblemCatalog.problemsFor(category, product).forEachIndexed { index, problem ->
            addProblemItem(problem, index > 0)
        }

        binding.continueButton.setOnClickListener {
            val problem = selectedProblem ?: return@setOnClickListener
            val intent = Intent(this, AddressSelectionActivity::class.java)
                .putExtra(ServiceCategoryActivity.EXTRA_SELECTED_CATEGORY, category.id)
                .putExtra(ProductSelectionActivity.EXTRA_SELECTED_PRODUCT, product.id)
                .putExtra(EXTRA_SELECTED_PROBLEM, problem.id)
            startActivity(intent)
        }
    }

    private fun addProblemItem(problem: Problem, addTopSpacing: Boolean) {
        val item = ItemProblemBinding.inflate(layoutInflater, binding.problemContainer, false)
        item.problemIcon.setImageResource(problem.iconRes)
        item.problemTitle.setText(problem.titleRes)
        item.problemDescription.setText(problem.descriptionRes)
        item.root.contentDescription = getString(problem.titleRes) + ". " + getString(problem.descriptionRes)
        item.root.setOnClickListener { selectProblem(problem) }

        if (addTopSpacing) {
            val params = item.root.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = resources.getDimensionPixelSize(R.dimen.home_quick_card_spacing)
            item.root.layoutParams = params
        }

        binding.problemContainer.addView(item.root)
        problemItems[problem] = item
    }

    private fun selectProblem(problem: Problem) {
        selectedProblem = problem
        problemItems.forEach { (candidate, item) -> item.root.isSelected = candidate == problem }
        binding.continueButton.isEnabled = true
    }

    private fun resolveCategory(id: String?): ServiceCategory =
        ServiceCategory.values().firstOrNull { it.id == id } ?: ServiceCategory.OTHER

    private fun resolveProduct(category: ServiceCategory, id: String?): Product {
        val products = ProductCatalog.productsFor(category)
        return products.firstOrNull { it.id == id } ?: products.first()
    }

    companion object {
        const val EXTRA_SELECTED_PROBLEM = "extra_selected_problem"
    }
}
