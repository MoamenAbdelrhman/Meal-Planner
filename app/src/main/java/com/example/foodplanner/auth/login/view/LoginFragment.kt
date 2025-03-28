package com.example.foodplanner.auth.login.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.foodplanner.main.view.MainActivity
import com.example.foodplanner.R
import com.example.foodplanner.auth.login.viewModel.LoginViewModel
import com.example.foodplanner.auth.login.viewModel.LoginViewModelFactory
import com.example.foodplanner.core.model.local.repository.UserRepositoryImpl
import com.example.foodplanner.core.model.local.source.LocalDataSourceImpl
import com.example.foodplanner.core.model.local.source.UserDatabase
import com.example.foodplanner.utils.NetworkUtils.isInternetAvailable
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var errorTextView: TextView

    private val viewModel: LoginViewModel by viewModels {
        val userRepository = UserRepositoryImpl(
            LocalDataSourceImpl(UserDatabase.getDatabaseInstance(requireContext()).userDao()),
            FirebaseAuth.getInstance()
        )
        LoginViewModelFactory(userRepository)
    }

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        } else {
            showToast(getString(R.string.google_sign_in_failed))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        setupGoogleSignIn()
        observeViewModel()

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            if (isInternetAvailable(requireContext()) && validateInputs(email, password)) {
                viewModel.loginUser(email, password)
            }
        }

        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        signupButton.setOnClickListener {
            if (!isAdded || isDetached) {
                Log.w("LoginFragment", "Cannot navigate: Fragment not attached to activity")
                Toast.makeText(requireContext(), "Fragment not ready, please try again", Toast.LENGTH_SHORT).show()
            } else if (findNavController().currentDestination?.id != R.id.loginFragment) {
                Log.w("LoginFragment", "Cannot navigate: Current destination is ${findNavController().currentDestination?.id}, expected ${R.id.loginFragment}")
                Toast.makeText(requireContext(), "Navigation state error, please try again", Toast.LENGTH_SHORT).show()
            } else {
                try {
                    findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
                } catch (e: IllegalArgumentException) {
                    Log.e("LoginFragment", "Navigation failed: ${e.message}", e)
                    Toast.makeText(requireContext(), "Navigation error, please try again", Toast.LENGTH_SHORT).show()
                }
            }
        }

        forgetPasswordButton.setOnClickListener {
            if (isAdded && !isDetached && findNavController().currentDestination?.id == R.id.loginFragment) {
                try {
                    findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
                } catch (e: IllegalArgumentException) {
                    Log.e("LoginFragment", "Navigation failed: ${e.message}", e)
                    Toast.makeText(requireContext(), "Navigation error, please try again", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Validate email and password inputs before login attempt
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

    // Set up Google Sign-In client with required options
    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    // Initiate Google Sign-In flow by launching the sign-in intent
    private fun signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener(requireActivity()) {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    // Handle the result of Google Sign-In and authenticate with Firebase
    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(ApiException::class.java)
            viewModel.firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            showToast(getString(R.string.google_sign_in_failed))
        }
    }

    // Observe ViewModel for login status, errors, and loading state
    private fun observeViewModel() {
        viewModel.loginSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Log.d("LoginFragment", "Login successful, navigating to MainActivity")
                viewModel.loginSuccess.removeObservers(viewLifecycleOwner)

                val intent = Intent(requireContext(), MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("IS_GUEST", false)
                }
                startActivity(intent)
                requireActivity().finish()
                Log.d("LoginFragment", "AuthActivity finished")
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            Log.d("LoginFragment", "Error received: $error")
            if (error.contains("Email not registered") || error.contains("Invalid password")) {
                checkIfGoogleAccount(emailEditText.text.toString().trim())
            } else {
                errorTextView.text = error
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    // Check if the email is associated with a Google account for sign-in
    private fun checkIfGoogleAccount(email: String) {
        if (email.isEmpty()) {
            errorTextView.text = getString(R.string.email_required)
            return
        }

        FirebaseAuth.getInstance().fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val signInMethods = task.result?.signInMethods
                    if (signInMethods != null && signInMethods.contains(GoogleAuthProvider.GOOGLE_SIGN_IN_METHOD)) {
                        errorTextView.text = "This account is linked to Google. Please use Google Sign-In."
                    } else if (signInMethods.isNullOrEmpty()) {
                        checkLocalUser(email)
                    } else {
                        errorTextView.text = "Invalid email or password. Please try again."
                    }
                } else {
                    errorTextView.text = "Error checking account type: ${task.exception?.message}"
                    showToast("Failed to check account. Try signing in with Google.")
                }
            }
    }

    // Check if the email exists in the local database for Google account linking
    private fun checkLocalUser(email: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val userRepository = UserRepositoryImpl(
                LocalDataSourceImpl(UserDatabase.getDatabaseInstance(requireContext()).userDao()),
                FirebaseAuth.getInstance()
            )
            val localUser = userRepository.getUserByEmail(email)
            if (localUser != null) {
                requireActivity().runOnUiThread {
                    errorTextView.text = "This account is linked to Google. Please use Google Sign-In."
                }
            } else {
                requireActivity().runOnUiThread {
                    errorTextView.text = "No account found with this email. Please sign up."
                }
            }
        }
    }

    // Display a toast message with the given text
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}