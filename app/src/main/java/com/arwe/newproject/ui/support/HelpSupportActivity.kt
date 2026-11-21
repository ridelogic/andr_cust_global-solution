package com.arwe.newproject.ui.support

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arwe.newproject.R
import com.arwe.newproject.databinding.ActivityHelpSupportBinding
import com.arwe.newproject.databinding.ItemFaqEntryBinding
import com.arwe.newproject.databinding.ItemSummaryBinding

/**
 * Static content only - no support API exists (no ticketing/chat backend), so this screen never
 * talks to the network. Phone/email are the real company contact details, shown as plain text
 * (no ACTION_DIAL/ACTION_SENDTO - not asked for, and dialing/emailing on the user's behalf is out
 * of scope here).
 */
class HelpSupportActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpSupportBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHelpSupportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener { finish() }

        bindSummary(binding.summaryPhone, R.drawable.ic_phone, R.string.help_support_phone_label, R.string.help_support_phone_value)
        bindSummary(binding.summaryEmail, R.drawable.ic_email, R.string.help_support_email_label, R.string.help_support_email_value)

        bindFaq(binding.faqBookService, R.string.help_support_faq_book_service_question, R.string.help_support_faq_book_service_answer)
        bindFaq(binding.faqMyServices, R.string.help_support_faq_my_services_question, R.string.help_support_faq_my_services_answer)
        bindFaq(binding.faqActiveService, R.string.help_support_faq_active_service_question, R.string.help_support_faq_active_service_answer)
        bindFaq(binding.faqServiceHistory, R.string.help_support_faq_service_history_question, R.string.help_support_faq_service_history_answer)
        bindFaq(binding.faqManageAddresses, R.string.help_support_faq_manage_addresses_question, R.string.help_support_faq_manage_addresses_answer)
    }

    private fun bindSummary(item: ItemSummaryBinding, iconRes: Int, labelRes: Int, valueRes: Int) {
        item.summaryIcon.setImageResource(iconRes)
        item.summaryLabel.setText(labelRes)
        item.summaryValue.setText(valueRes)
    }

    private fun bindFaq(item: ItemFaqEntryBinding, questionRes: Int, answerRes: Int) {
        item.faqQuestion.setText(questionRes)
        item.faqAnswer.setText(answerRes)
    }
}
