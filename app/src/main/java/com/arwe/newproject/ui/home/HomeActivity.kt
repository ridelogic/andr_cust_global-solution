package com.arwe.newproject.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.arwe.newproject.R
import com.arwe.newproject.databinding.ActivityHomeBinding
import com.arwe.newproject.session.SessionManager
import com.arwe.newproject.ui.booking.ServiceCategoryActivity
import com.arwe.newproject.ui.common.PlaceholderActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val sessionManager by lazy { SessionManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.greetingText.text = sessionManager.getCustomerName()
            ?.let { getString(R.string.home_greeting_named, it) }
            ?: getString(R.string.home_greeting_fallback)

        binding.notificationBell.setOnClickListener {
            // UI only; FCM/notifications are not implemented yet.
            Toast.makeText(this, R.string.home_notifications_placeholder, Toast.LENGTH_SHORT).show()
        }

        binding.cardBookService.setOnClickListener {
            startActivity(Intent(this, ServiceCategoryActivity::class.java))
        }
        binding.cardMyServices.setOnClickListener { openPlaceholder(R.string.quick_service_my_services) }
        binding.cardActiveService.setOnClickListener { openPlaceholder(R.string.quick_service_active) }
        binding.cardServiceHistory.setOnClickListener { openPlaceholder(R.string.quick_service_history) }

        binding.bottomNavigation.selectedItemId = R.id.nav_home
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_my_services -> {
                    openPlaceholder(R.string.quick_service_my_services)
                    false
                }
                R.id.nav_support -> {
                    openPlaceholder(R.string.nav_support)
                    false
                }
                R.id.nav_profile -> {
                    openPlaceholder(R.string.nav_profile)
                    false
                }
                else -> false
            }
        }
    }

    private fun openPlaceholder(titleRes: Int) {
        startActivity(PlaceholderActivity.newIntent(this, getString(titleRes)))
    }
}
