package com.arwe.newproject.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.arwe.newproject.databinding.ActivitySplashBinding
import com.arwe.newproject.session.SessionManager
import com.arwe.newproject.ui.home.HomeActivity
import com.arwe.newproject.ui.login.LoginActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val sessionManager by lazy { SessionManager(this) }
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handler.postDelayed({ navigateNext() }, SPLASH_DELAY_MS)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun navigateNext() {
        val destination = if (sessionManager.isLoggedIn()) {
            HomeActivity::class.java
        } else {
            LoginActivity::class.java
        }
        startActivity(Intent(this, destination))
        finish()
    }

    companion object {
        private const val SPLASH_DELAY_MS = 1500L
    }
}
