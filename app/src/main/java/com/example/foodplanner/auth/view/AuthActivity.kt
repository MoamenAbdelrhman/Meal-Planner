package com.example.foodplanner.auth.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.foodplanner.R
import com.example.foodplanner.core.util.CreateMaterialAlertDialogBuilder
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


        // Initialize NavHostFragment and NavController
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)

        navController = (navHostFragment as NavHostFragment).navController

        auth = FirebaseAuth.getInstance()

        // Check if a user is already logged in and redirect to MainActivity if true
        if (auth.currentUser != null) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("IS_GUEST", false)
            }
            startActivity(intent)
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (navController.popBackStack()) {
            return
        }

        CreateMaterialAlertDialogBuilder.createExitOrContinueAsGuestDialog(
            context = this,
            onExit = {
                Toast.makeText(this, "Exiting the app", Toast.LENGTH_SHORT).show()
                super.onBackPressed()
            },
            onContinueAsGuest = {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("IS_GUEST", true)
                }
                startActivity(intent)
                finish()
            },
            onCancel = {
                if (navController.currentDestination?.id != R.id.loginFragment) {
                    try {
                        navController.navigate(R.id.loginFragment)
                    } catch (e: IllegalStateException) {
                        Toast.makeText(this,"Failed to reset to LoginFragment: ${e.message}",Toast.LENGTH_SHORT).show()

                    }
                }
            }
        )
    }
}