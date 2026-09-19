package com.arwe.newproject.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.arwe.newproject.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.clickButton.setOnClickListener {
            Toast.makeText(this, "Button Clicked", Toast.LENGTH_SHORT).show()

        }
        println("Life_Cycle"+"onCreate")
    }
    override fun onStart() {
        super.onStart()
        println("Life_Cycle"+"onStart")
    }


    override fun onResume() {
        super.onResume()
        println("Life_Cycle"+"onResume")
    }

    override fun onPause() {
        super.onPause()
        println("Life_Cycle"+"onPause")
    }

    override fun onStop() {
        super.onStop()
        println("Life_Cycle"+"onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        println("Life_Cycle"+"onDestroy")
    }


}