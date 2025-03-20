package com.example.foodplanner

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import android.view.animation.AnimationSet
import com.example.foodplanner.auth.AuthActivity
import com.example.foodplanner.auth.AuthViewModel
import com.example.foodplanner.auth.AuthViewModelFactory
import com.example.foodplanner.core.model.local.repository.UserRepositoryImpl
import com.example.foodplanner.core.model.local.source.LocalDataSourceImpl
import com.example.foodplanner.core.model.local.source.UserDatabase
import com.example.foodplanner.main.view.MainActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_splash)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splash)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val logo = findViewById<ImageView>(R.id.splash_logo)
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val rotate = AnimationUtils.loadAnimation(this, R.anim.rotate)
        val animationSet = AnimationSet(true)
        animationSet.addAnimation(fadeIn)
        animationSet.addAnimation(rotate)
        logo.startAnimation(animationSet)

        val splashText = findViewById<TextView>(R.id.splash_text)
        val welcomeMessage = "Welcome to Meal Planner"
        val animator = ValueAnimator.ofInt(0, welcomeMessage.length)
        animator.duration = 1300
        animator.addUpdateListener { animation ->
            val end = animation.animatedValue as Int
            splashText.text = welcomeMessage.substring(0, end)
        }
        animator.startDelay = 1000
        animator.start()

        val userRepository = UserRepositoryImpl(
            LocalDataSourceImpl(UserDatabase.getDatabaseInstance(this).userDao()),
            FirebaseAuth.getInstance()
        )
        val authViewModelFactory = AuthViewModelFactory(userRepository)
        authViewModel = ViewModelProvider(this, authViewModelFactory)[AuthViewModel::class.java]

        authViewModel.checkUserLoggedIn()
        authViewModel.user.observe(this) { user ->
            Handler(Looper.getMainLooper()).postDelayed({
                if (user != null) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    val intent = Intent(this, AuthActivity::class.java)
                    startActivity(intent)
                }
                finish()
            }, 2600)
        }
    }
}