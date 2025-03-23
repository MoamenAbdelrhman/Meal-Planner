package com.example.foodplanner.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.foodplanner.R
import com.example.foodplanner.main.view.MainActivity
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth)

        Log.d("AuthActivity", "Layout set, attempting to find NavHostFragment")

        // الحصول على NavHostFragment باستخدام supportFragmentManager
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (navHostFragment == null) {
            Log.e("AuthActivity", "No NavHostFragment found with ID fragmentContainer")
            throw IllegalStateException("No NavHostFragment found with ID fragmentContainer in activity_auth.xml")
        } else {
            Log.d("AuthActivity", "NavHostFragment found: $navHostFragment")
        }

        // تهيئة NavController من NavHostFragment
        navController = (navHostFragment as NavHostFragment).navController
        Log.d("AuthActivity", "NavController initialized: $navController")

        auth = FirebaseAuth.getInstance()

        // التحقق من وجود مستخدم مسجل مسبقًا
        if (auth.currentUser != null) {
            Log.d("AuthActivity", "User already logged in, navigating to MainActivity")
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (!navController.popBackStack()) {
            finish()
        } else {
            super.onBackPressed()
        }
    }
}