package com.arwe.newproject.ui.profile

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.arwe.newproject.R
import com.arwe.newproject.databinding.ActivityProfileBinding
import com.arwe.newproject.databinding.ItemProfileMenuBinding
import com.arwe.newproject.session.SessionManager
import com.arwe.newproject.ui.addresses.MyAddressesActivity
import com.arwe.newproject.ui.common.PlaceholderActivity
import com.arwe.newproject.ui.login.LoginActivity
import com.arwe.newproject.ui.settings.AppSettingsActivity
import com.arwe.newproject.ui.support.HelpSupportActivity

/**
 * Reads customer details from the existing SessionManager only - no separate GET
 * customer/profile call exists yet, and SessionManager already carries everything this screen
 * shows (see its own doc on saveSession()).
 */
class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val sessionManager by lazy { SessionManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }

        bindProfileHeader()
        bindMenu()
    }

    private fun bindProfileHeader() {
        binding.profileNameText.text = sessionManager.getCustomerName()?.takeIf { it.isNotBlank() }
            ?: getString(R.string.profile_name_fallback)

        binding.profileEmailText.text = sessionManager.getCustomerEmail()?.takeIf { it.isNotBlank() }
            ?: getString(R.string.profile_value_not_available)

        binding.profileMobileText.text = sessionManager.getCustomerMobile()?.takeIf { it.isNotBlank() }
            ?: getString(R.string.profile_value_not_available)

        val customerCode = sessionManager.getCustomerCode()?.takeIf { it.isNotBlank() }
        if (customerCode != null) {
            binding.profileCustomerCodeText.text = getString(R.string.profile_customer_code_format, customerCode)
            binding.profileCustomerCodeText.visibility = View.VISIBLE
        } else {
            binding.profileCustomerCodeText.visibility = View.GONE
        }
    }

    private fun bindMenu() {
        bindMenuItem(binding.menuPersonalInformation, R.drawable.ic_account, R.string.profile_menu_personal_information) {
            startActivity(Intent(this, PersonalInformationActivity::class.java))
        }
        bindMenuItem(binding.menuMyAddresses, R.drawable.ic_location, R.string.profile_menu_my_addresses) {
            startActivity(Intent(this, MyAddressesActivity::class.java))
        }
        bindMenuItem(binding.menuAppSettings, R.drawable.ic_settings, R.string.profile_menu_app_settings) {
            startActivity(Intent(this, AppSettingsActivity::class.java))
        }
        bindMenuItem(binding.menuHelpSupport, R.drawable.ic_nav_support, R.string.profile_menu_help_support) {
            startActivity(Intent(this, HelpSupportActivity::class.java))
        }
        bindMenuItem(binding.menuAbout, R.drawable.ic_info, R.string.profile_menu_about) {
            startActivity(Intent(this, AboutActivity::class.java))
        }
        bindMenuItem(binding.menuLogout, R.drawable.ic_logout, R.string.profile_menu_logout, isDestructive = true) {
            logout()
        }
    }

    private fun bindMenuItem(
        item: ItemProfileMenuBinding,
        iconRes: Int,
        labelRes: Int,
        isDestructive: Boolean = false,
        onClick: () -> Unit
    ) {
        item.menuIcon.setImageResource(iconRes)
        item.menuLabel.setText(labelRes)
        if (isDestructive) {
            val destructiveColor = ContextCompat.getColor(this, R.color.notification_dot)
            item.menuIcon.imageTintList = ColorStateList.valueOf(destructiveColor)
            item.menuLabel.setTextColor(destructiveColor)
            item.menuChevron.visibility = View.GONE
        }
        item.root.setOnClickListener { onClick() }
    }

    private fun openPlaceholder(titleRes: Int) {
        startActivity(PlaceholderActivity.newIntent(this, getString(titleRes)))
    }

    private fun logout() {
        sessionManager.clearSession()
        val intent = Intent(this, LoginActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}
