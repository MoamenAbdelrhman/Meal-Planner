package com.example.foodplanner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.foodplanner.auth.AuthActivity
import com.example.foodplanner.main.view.MainActivity

class GuestOrLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_guest_or_login)

        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

        findViewById<Button>(R.id.btn_login).setOnClickListener {
            val intent = Intent(this, AuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            with(prefs.edit()) {
                putBoolean("isFirstRun", false)
                apply()
            }
            finish()
        }

        findViewById<Button>(R.id.btn_guest).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("IS_GUEST", true)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            with(prefs.edit()) {
                putBoolean("isFirstRun", false)
                apply()
            }
            finish()
        }
    }
}