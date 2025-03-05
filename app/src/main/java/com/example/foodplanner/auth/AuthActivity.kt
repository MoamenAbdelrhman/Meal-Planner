package com.example.foodplanner.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.foodplanner.R
import com.example.foodplanner.auth.Login.view.LoginFragment
import com.example.foodplanner.main.view.MainActivity
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()


        // Check if user is already logged in
        if (auth.currentUser != null) {
            navigateToMainScreen()
        } else {
            loadFragment(LoginFragment())
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    /**
     * Navigate to the main activity if the user is logged in.
     */
    private fun navigateToMainScreen() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    /**
     * Navigate to SignupFragment.
     */
    fun navigateToSignup() {
        loadFragment(SignupFragment())
    }

    /**
     * Navigate to LoginFragment.
     */

    fun navigateToLogin() {
        loadFragment(LoginFragment())
    }

    /**
     * Navigate to ForgotPasswordFragment.
     */
    fun navigateToForgetPassword() {
        loadFragment(ForgotPasswordFragment())
    }
}