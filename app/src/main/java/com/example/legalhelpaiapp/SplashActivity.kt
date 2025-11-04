package com.example.legalhelpaiapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.legalhelpaiapp.ui.login.LoginActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Using a Handler to delay the transition
        Handler(Looper.getMainLooper()).postDelayed({
            // Start the next activity (e.g., MainActivity or LoginActivity)
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Close the splash activity
        }, 3000) // 3000ms = 3 seconds delay
    }
}