package com.arwe.newproject.ui.booking

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arwe.newproject.databinding.ActivityServiceCategoryBinding
import com.arwe.newproject.databinding.ItemServiceCategoryBinding

class ServiceCategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServiceCategoryBinding
    private var selectedCategory: ServiceCategory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServiceCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }

        bindCategory(binding.categoryBattery, ServiceCategory.BATTERY)
        bindCategory(binding.categoryHomeAppliance, ServiceCategory.HOME_APPLIANCE)
        bindCategory(binding.categoryElectrical, ServiceCategory.ELECTRICAL)
        bindCategory(binding.categoryPlumbing, ServiceCategory.PLUMBING)
        bindCategory(binding.categoryOther, ServiceCategory.OTHER)

        binding.continueButton.setOnClickListener {
            val category = selectedCategory ?: return@setOnClickListener
            val intent = Intent(this, ProductSelectionActivity::class.java)
                .putExtra(EXTRA_SELECTED_CATEGORY, category.id)
            startActivity(intent)
        }
    }

    private fun bindCategory(item: ItemServiceCategoryBinding, category: ServiceCategory) {
        item.categoryIcon.setImageResource(category.iconRes)
        item.categoryTitle.setText(category.titleRes)
        item.categoryDescription.setText(category.descriptionRes)
        item.root.contentDescription = getString(category.titleRes) + ". " + getString(category.descriptionRes)
        item.root.setOnClickListener { selectCategory(category) }
    }

    private fun selectCategory(category: ServiceCategory) {
        selectedCategory = category
        updateSelectionState()
    }

    private fun updateSelectionState() {
        binding.categoryBattery.root.isSelected = selectedCategory == ServiceCategory.BATTERY
        binding.categoryHomeAppliance.root.isSelected = selectedCategory == ServiceCategory.HOME_APPLIANCE
        binding.categoryElectrical.root.isSelected = selectedCategory == ServiceCategory.ELECTRICAL
        binding.categoryPlumbing.root.isSelected = selectedCategory == ServiceCategory.PLUMBING
        binding.categoryOther.root.isSelected = selectedCategory == ServiceCategory.OTHER
        binding.continueButton.isEnabled = selectedCategory != null
    }

    companion object {
        const val EXTRA_SELECTED_CATEGORY = "extra_selected_category"
    }
}
