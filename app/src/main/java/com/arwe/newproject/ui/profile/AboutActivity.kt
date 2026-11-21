package com.arwe.newproject.ui.profile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arwe.newproject.BuildConfig
import com.arwe.newproject.R
import com.arwe.newproject.databinding.ActivityAboutBinding

/** Static content only - app version comes from the real BuildConfig.VERSION_NAME, never a
 * hardcoded string; phone/email reuse the same real contact strings HelpSupportActivity shows. */
class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }

        binding.versionText.text = getString(R.string.about_version_format, BuildConfig.VERSION_NAME)

        binding.summaryPhone.summaryIcon.setImageResource(R.drawable.ic_phone)
        binding.summaryPhone.summaryLabel.setText(R.string.help_support_phone_label)
        binding.summaryPhone.summaryValue.setText(R.string.help_support_phone_value)

        binding.summaryEmail.summaryIcon.setImageResource(R.drawable.ic_email)
        binding.summaryEmail.summaryLabel.setText(R.string.help_support_email_label)
        binding.summaryEmail.summaryValue.setText(R.string.help_support_email_value)
    }
}
