package com.example.foodplanner.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.example.foodplanner.MainActivity
import com.example.foodplanner.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginFragment : Fragment() {

    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: Button
    private lateinit var googleSignInButton: Button
    private lateinit var signupButton: TextView
    private lateinit var forgetPasswordButton: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var errorTextView: TextView

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        } else {
            Log.w("GoogleSignIn", "Google Sign-In failed")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Initialize UI components
        emailEditText = view.findViewById(R.id.emailEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        loginButton = view.findViewById(R.id.loginButton)
        googleSignInButton = view.findViewById(R.id.googleSignInButton)
        signupButton = view.findViewById(R.id.signupButton)
        forgetPasswordButton = view.findViewById(R.id.textViewForgetPassword)
        progressBar = view.findViewById(R.id.progressBar)
        emailInputLayout = view.findViewById(R.id.emailInputLayout)
        passwordInputLayout = view.findViewById(R.id.passwordInputLayout)
        errorTextView = view.findViewById(R.id.errorTextView)

        FirebaseApp.initializeApp(requireContext())
        // Initialize FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        val currentUser = auth.currentUser

        if (currentUser != null) {
            // If user is logged in, navigate to MainActivity
            startActivity(Intent(requireContext(), MainActivity::class.java))
            requireActivity().finish()
        }
        // Clear errors dynamically
        emailEditText.addTextChangedListener { emailInputLayout.error = null }
        passwordEditText.addTextChangedListener { passwordInputLayout.error = null }

        // Set click listeners
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            if (validateInputs(email, password)) {
                loginUser(email, password)
            }
        }

        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        signupButton.setOnClickListener {
            (activity as AuthActivity).navigateToSignup()
        }

        forgetPasswordButton.setOnClickListener {
            (activity as AuthActivity).navigateToForgetPassword()
        }
    }



    private fun signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener(requireActivity()) {
            val signInIntent = googleSignInClient.signInIntent
            Log.d("GoogleSignIn", "Starting Google Sign-In")
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)

            Firebase.auth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity()) { signInTask ->
                    if (signInTask.isSuccessful) {
                        navigateToMain()
                    } else {
                        showToast(getString(R.string.auth_failed))
                    }
                }
        } catch (e: ApiException) {
            Log.w("GoogleSignIn", "Sign-in failed: ${e.statusCode}")
            showToast(getString(R.string.google_sign_in_failed))
        }
    }

    private fun validateInputs(email: String, password: String): Boolean {
        return when {
            email.isEmpty() -> {
                emailInputLayout.error = getString(R.string.email_required)
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                emailInputLayout.error = getString(R.string.invalid_email)
                false
            }
            password.isEmpty() -> {
                passwordInputLayout.error = getString(R.string.password_required)
                false
            }
            password.length < 6 -> {
                passwordInputLayout.error = getString(R.string.password_length)
                false
            }
            else -> true
        }
    }

    private fun loginUser(email: String, password: String) {
        progressBar.visibility = View.VISIBLE

        Firebase.auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                progressBar.visibility = View.GONE
                if (task.isSuccessful) {
                    Log.d("Auth", "Login successful: ${auth.currentUser?.email}")
                    showToast(getString(R.string.login_successful))
                    navigateToMain()
                } else {
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthInvalidUserException -> getString(R.string.email_not_registered)
                        is FirebaseAuthInvalidCredentialsException -> getString(R.string.invalid_password)
                        else -> task.exception?.message ?: getString(R.string.auth_failed)                    }
                    showToast(errorMessage)
                }
            }
    }


    private fun navigateToMain() {
        startActivity(Intent(requireContext(), MainActivity::class.java))
        requireActivity().finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
