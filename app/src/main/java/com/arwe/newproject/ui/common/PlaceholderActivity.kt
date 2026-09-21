package com.arwe.newproject.ui.common

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arwe.newproject.databinding.ActivityPlaceholderBinding

class PlaceholderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlaceholderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaceholderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.placeholderTitle.text = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        binding.backButton.setOnClickListener { finish() }
    }

    companion object {
        private const val EXTRA_TITLE = "extra_title"

        fun newIntent(context: Context, title: String): Intent =
            Intent(context, PlaceholderActivity::class.java).putExtra(EXTRA_TITLE, title)
    }
}
