package com.arwe.newproject.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arwe.newproject.BuildConfig
import com.arwe.newproject.R
import com.arwe.newproject.databinding.ActivityAppSettingsBinding
import com.arwe.newproject.ui.profile.AboutActivity

/**
 * Only exposes settings that actually exist in this app - confirmed by inspection, not assumed:
 * no notification preference/toggle exists anywhere (the Home bell is a static UI placeholder,
 * not a persisted setting - see HomeActivity's own comment on it), no DataStore/preferences
 * screen exists, and no privacy screen/content exists. Notifications and Privacy are therefore
 * shown as plain, non-interactive info rows stating they are not yet available - never a fake
 * switch or invented legal text. App Version is real (BuildConfig.VERSION_NAME). About is a real,
 * working navigation to the existing AboutActivity.
 */
class AppSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppSettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }

        binding.summaryNotifications.summaryIcon.setImageResource(R.drawable.ic_notification)
        binding.summaryNotifications.summaryLabel.setText(R.string.app_settings_label_notifications)
        binding.summaryNotifications.summaryValue.setText(R.string.app_settings_value_notifications_unavailable)

        binding.summaryVersion.summaryIcon.setImageResource(R.drawable.ic_info)
        binding.summaryVersion.summaryLabel.setText(R.string.app_settings_label_version)
        binding.summaryVersion.summaryValue.text = getString(R.string.about_version_format, BuildConfig.VERSION_NAME)

        binding.summaryPrivacy.summaryIcon.setImageResource(R.drawable.ic_lock)
        binding.summaryPrivacy.summaryLabel.setText(R.string.app_settings_label_privacy)
        binding.summaryPrivacy.summaryValue.setText(R.string.app_settings_value_privacy_unavailable)

        binding.menuAbout.menuIcon.setImageResource(R.drawable.ic_info)
        binding.menuAbout.menuLabel.setText(R.string.profile_menu_about)
        binding.menuAbout.root.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }
}
