package com.example.foodplanner.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.foodplanner.R
import com.example.foodplanner.main.view.MainActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var navController: NavController
    private var isGuest: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth)

        // Read guest status from Intent
        isGuest = intent.getBooleanExtra("IS_GUEST", false)

        Log.d("AuthActivity", "Layout set, attempting to find NavHostFragment")

        // Initialize NavHostFragment and NavController
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (navHostFragment == null) {
            Log.e("AuthActivity", "No NavHostFragment found with ID fragmentContainer")
            throw IllegalStateException("No NavHostFragment found with ID fragmentContainer in activity_auth.xml")
        } else {
            Log.d("AuthActivity", "NavHostFragment found: $navHostFragment")
        }

        navController = (navHostFragment as NavHostFragment).navController
        Log.d("AuthActivity", "NavController initialized: $navController")

        auth = FirebaseAuth.getInstance()

        // Check if a user is already logged in and redirect to MainActivity if true
        if (auth.currentUser != null) {
            Log.d("AuthActivity", "User already logged in, navigating to MainActivity")
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("IS_GUEST", false)
            }
            startActivity(intent)
            finish()
        }
    }

    // Handle navigation up action for the NavController
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    // Handle back press to either pop the back stack or show a dialog to exit/continue as guest
    override fun onBackPressed() {
        if (!navController.popBackStack()) {
            MaterialAlertDialogBuilder(this)
                .setMessage("Do you want to exit the app or continue as a guest?")
                .setPositiveButton("Continue as Guest") { _, _ ->
                    // Navigate to MainActivity as a guest and clear the task stack
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra("IS_GUEST", true)
                    }
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Exit") { _, _ ->
                    // Show a toast and close all activities in the current task
                    Toast.makeText(this, "Exiting the app", Toast.LENGTH_SHORT).show()
                    finishAffinity()
                }
                .setCancelable(true)
                .show()
        } else {
            super.onBackPressed()
        }
    }
}