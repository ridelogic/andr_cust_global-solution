package com.arwe.newproject.ui.profile

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arwe.newproject.R
import com.arwe.newproject.databinding.ActivityPersonalInformationBinding
import com.arwe.newproject.databinding.ItemSummaryBinding
import com.arwe.newproject.session.SessionManager

/**
 * Read-only - there is no confirmed customer profile UPDATE API yet, so this screen only ever
 * displays what SessionManager already holds (the same fields ProfileActivity's header shows;
 * see SessionManager.saveSession()'s own doc). No edit/save affordance, no second API surface.
 */
class PersonalInformationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPersonalInformationBinding
    private val sessionManager by lazy { SessionManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonalInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }

        bindSummary(binding.summaryFullName, R.drawable.ic_account, R.string.personal_information_label_full_name, sessionManager.getCustomerName())
        bindSummary(binding.summaryCustomerCode, R.drawable.ic_info, R.string.personal_information_label_customer_code, sessionManager.getCustomerCode())
        bindSummary(binding.summaryMobile, R.drawable.ic_phone, R.string.personal_information_label_mobile, sessionManager.getCustomerMobile())
        bindSummary(binding.summaryEmail, R.drawable.ic_email, R.string.personal_information_label_email, sessionManager.getCustomerEmail())
    }

    private fun bindSummary(item: ItemSummaryBinding, iconRes: Int, labelRes: Int, value: String?) {
        item.summaryIcon.setImageResource(iconRes)
        item.summaryLabel.setText(labelRes)
        item.summaryValue.text = displayValueOrNotAvailable(value, getString(R.string.profile_value_not_available))
    }
}

/** Null/blank SessionManager fields (e.g. no email on file) show [notAvailable] rather than a
 * blank row - never invented data. */
internal fun displayValueOrNotAvailable(value: String?, notAvailable: String): String =
    value?.takeIf { it.isNotBlank() } ?: notAvailable
