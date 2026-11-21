package com.arwe.newproject.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.arwe.newproject.BuildConfig
import com.arwe.newproject.R
import com.arwe.newproject.databinding.ActivityHomeBinding
import com.arwe.newproject.databinding.ItemNavDrawerMenuBinding
import com.arwe.newproject.session.SessionManager
import com.arwe.newproject.ui.activeservice.ActiveServiceActivity
import com.arwe.newproject.ui.booking.ServiceCategoryActivity
import com.arwe.newproject.ui.common.PlaceholderActivity
import com.arwe.newproject.ui.login.LoginActivity
import com.arwe.newproject.ui.myservices.MyServicesActivity
import com.arwe.newproject.ui.profile.ProfileActivity
import com.arwe.newproject.ui.servicehistory.ServiceHistoryActivity
import com.arwe.newproject.ui.settings.AppSettingsActivity
import com.arwe.newproject.ui.support.HelpSupportActivity

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
        binding.cardMyServices.setOnClickListener { startActivity(Intent(this, MyServicesActivity::class.java)) }
        binding.cardActiveService.setOnClickListener { startActivity(Intent(this, ActiveServiceActivity::class.java)) }
        binding.cardServiceHistory.setOnClickListener { startActivity(Intent(this, ServiceHistoryActivity::class.java)) }

        binding.bottomNavigation.selectedItemId = R.id.nav_home
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_my_services -> {
                    startActivity(Intent(this, MyServicesActivity::class.java))
                    false
                }
                R.id.nav_support -> {
                    openPlaceholder(R.string.nav_support)
                    false
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    false
                }
                else -> false
            }
        }

        setupDrawer()
    }

    private fun setupDrawer() {
        // "~80% of screen width" (see the task's own reference) - a percentage DrawerLayout side
        // panel can't be expressed purely in XML, so it's computed here; nav_drawer_default_width
        // in the layout is only the pre-measurement fallback.
        val drawerWidth = (resources.displayMetrics.widthPixels * 0.8f).toInt()
        binding.navDrawerContent.root.layoutParams = binding.navDrawerContent.root.layoutParams.apply {
            width = drawerWidth
        }

        binding.menuButton.setOnClickListener { binding.drawerLayout.openDrawer(GravityCompat.START) }

        val drawer = binding.navDrawerContent
        drawer.drawerCustomerNameText.text = sessionManager.getCustomerName()?.takeIf { it.isNotBlank() }
            ?: getString(R.string.profile_name_fallback)
        drawer.drawerVersionText.text = getString(R.string.nav_drawer_version_format, BuildConfig.VERSION_NAME)

        // Home is always the current destination here - this drawer only exists on HomeActivity -
        // so it is the one row given the "current destination" highlight, and its tap just closes
        // the drawer rather than relaunching the screen already showing.
        bindDrawerItem(drawer.drawerMenuHome, R.drawable.ic_nav_home, R.string.nav_home, isSelected = true) {
            closeDrawer()
        }
        bindDrawerItem(drawer.drawerMenuBookService, R.drawable.ic_book_service, R.string.quick_service_book) {
            closeDrawer()
            startActivity(Intent(this, ServiceCategoryActivity::class.java))
        }
        bindDrawerItem(drawer.drawerMenuMyServices, R.drawable.ic_my_services, R.string.quick_service_my_services) {
            closeDrawer()
            startActivity(Intent(this, MyServicesActivity::class.java))
        }
        bindDrawerItem(drawer.drawerMenuActiveService, R.drawable.ic_active_service, R.string.quick_service_active) {
            closeDrawer()
            startActivity(Intent(this, ActiveServiceActivity::class.java))
        }
        bindDrawerItem(drawer.drawerMenuServiceHistory, R.drawable.ic_service_history, R.string.quick_service_history) {
            closeDrawer()
            startActivity(Intent(this, ServiceHistoryActivity::class.java))
        }
        bindDrawerItem(drawer.drawerMenuProfile, R.drawable.ic_account, R.string.nav_profile) {
            closeDrawer()
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        bindDrawerItem(drawer.drawerMenuSettings, R.drawable.ic_settings, R.string.nav_drawer_settings) {
            closeDrawer()
            startActivity(Intent(this, AppSettingsActivity::class.java))
        }
        bindDrawerItem(drawer.drawerMenuHelpSupport, R.drawable.ic_nav_support, R.string.profile_menu_help_support) {
            closeDrawer()
            startActivity(Intent(this, HelpSupportActivity::class.java))
        }
        bindDrawerItem(drawer.drawerMenuLogout, R.drawable.ic_logout, R.string.profile_menu_logout) {
            closeDrawer()
            logout()
        }
    }

    private fun bindDrawerItem(
        item: ItemNavDrawerMenuBinding,
        iconRes: Int,
        labelRes: Int,
        isSelected: Boolean = false,
        onClick: () -> Unit
    ) {
        item.drawerItemIcon.setImageResource(iconRes)
        item.drawerItemLabel.setText(labelRes)
        if (isSelected) {
            item.root.setBackgroundResource(R.drawable.bg_nav_drawer_item_selected)
        }
        item.root.setOnClickListener { onClick() }
    }

    private fun closeDrawer() {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun logout() {
        sessionManager.clearSession()
        val intent = Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun openPlaceholder(titleRes: Int) {
        startActivity(PlaceholderActivity.newIntent(this, getString(titleRes)))
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
