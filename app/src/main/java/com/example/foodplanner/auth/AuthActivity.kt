package com.example.foodplanner.auth

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.foodplanner.R
import com.google.firebase.auth.FirebaseAuth

class AuthActivity : AppCompatActivity() {

    private lateinit var loginFragment: LoginFragment
    private lateinit var fragmentManager: FragmentManager
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


        fragmentManager = supportFragmentManager

        loadFragment(LoginFragment())
    }
    fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    // Function to navigate to SignupFragment
    fun navigateToSignup() {
        loadFragment(SignupFragment())
    }

    // Function to navigate to LoginFragment
    fun navigateToLogin() {
        loadFragment(LoginFragment())
    }
    // Function to navigate to ForgetPassword
    fun navigateToForgetPassword() {
        loadFragment(ForgotPasswordFragment())
    }
}