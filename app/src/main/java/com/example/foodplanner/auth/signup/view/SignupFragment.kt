package com.example.foodplanner.auth.signup.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.foodplanner.R
import com.example.foodplanner.auth.signup.viewModel.SignupViewModel
import com.example.foodplanner.auth.signup.viewModel.SignupViewModelFactory
import com.example.foodplanner.core.model.local.repository.UserRepositoryImpl
import com.example.foodplanner.core.model.local.source.LocalDataSourceImpl
import com.example.foodplanner.core.model.local.source.UserDatabase
import com.example.foodplanner.main.view.MainActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class SignupFragment : Fragment() {

    private val viewModel: SignupViewModel by viewModels {
        val userRepository = UserRepositoryImpl(
            LocalDataSourceImpl(UserDatabase.getDatabaseInstance(requireContext()).userDao()),
            FirebaseAuth.getInstance()
        )
        SignupViewModelFactory(userRepository)
    }

    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var usernameInputLayout: TextInputLayout
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var confirmPasswordInputLayout: TextInputLayout
    private lateinit var signupButtonS: Button
    private lateinit var loginButton: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorTextView: TextView

    private var isValidationStarted = false

    private val googleSignUpLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (
            result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    viewModel.signUpWithGoogle(account.idToken!!)
                } else {
                    Toast.makeText(context, "Google sign-in failed: No account", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Toast.makeText(context, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Google sign-in canceled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_signup, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Views
        usernameEditText = view.findViewById(R.id.nameEditText)
        emailEditText = view.findViewById(R.id.emailEditText)
        passwordEditText = view.findViewById(R.id.passwordEditText)
        confirmPasswordEditText = view.findViewById(R.id.confirmPasswordEditText)
        usernameInputLayout = view.findViewById(R.id.nameInputLayout)
        emailInputLayout = view.findViewById(R.id.emailInputLayout)
        passwordInputLayout = view.findViewById(R.id.passwordInputLayout)
        confirmPasswordInputLayout = view.findViewById(R.id.confirmPasswordInputLayout)
        signupButtonS = view.findViewById(R.id.signupButtonS)
        loginButton = view.findViewById(R.id.loginButton)
        progressBar = view.findViewById(R.id.progressBar)
        errorTextView = view.findViewById(R.id.errorTextView)

        // Setup TextWatcher
        setupTextWatchers()

        // Signup Button Click
        signupButtonS.setOnClickListener {
            isValidationStarted = true

            val name = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            usernameInputLayout.error = null
            emailInputLayout.error = null
            passwordInputLayout.error = null
            confirmPasswordInputLayout.error = null

            val validationResult = viewModel.validateInputs(name, email, password, confirmPassword)
            if (validationResult.isValid) {
                viewModel.signupUser(email, password, name)
            } else {
                usernameInputLayout.error = validationResult.nameError
                emailInputLayout.error = validationResult.emailError
                passwordInputLayout.error = validationResult.passwordError
                confirmPasswordInputLayout.error = validationResult.confirmPasswordError
            }
        }

        // Navigate to Login Fragment
        loginButton.setOnClickListener {
            if (isAdded && !isDetached) {
                try {
                    findNavController().popBackStack(R.id.loginFragment, false)
                } catch (e: IllegalStateException) {
                    Toast.makeText(requireContext(), "Navigation error, please try again", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Google Sign-Up Button Click
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.googleSignUpButton).setOnClickListener {
            signUpWithGoogle()
        }

        // Observe ViewModel
        viewModel.signupSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireActivity(), "Signup successful!", Toast.LENGTH_SHORT).show()

                val intent = Intent(requireContext(), MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("IS_GUEST", false)
                }
                startActivity(intent)
                requireActivity().finish()
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                Log.d("SignupFragment", "Error received: $it")
                errorTextView.text = it
            }
        }

        viewModel.googleSignUpSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(context, "Google login successful", Toast.LENGTH_SHORT).show()
                val intent = Intent(requireContext(), MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("IS_GUEST", false)
                }
                startActivity(intent)
                requireActivity().finish()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isValidationStarted) {
                    val name = usernameEditText.text.toString().trim()
                    val email = emailEditText.text.toString().trim()
                    val password = passwordEditText.text.toString().trim()
                    val confirmPassword = confirmPasswordEditText.text.toString().trim()

                    val validationResult = viewModel.validateInputs(name, email, password, confirmPassword)

                    usernameInputLayout.error = validationResult.nameError
                    emailInputLayout.error = validationResult.emailError
                    passwordInputLayout.error = validationResult.passwordError
                    confirmPasswordInputLayout.error = validationResult.confirmPasswordError
                }
            }
        }

        usernameEditText.addTextChangedListener(textWatcher)
        emailEditText.addTextChangedListener(textWatcher)
        passwordEditText.addTextChangedListener(textWatcher)
        confirmPasswordEditText.addTextChangedListener(textWatcher)
    }

    private fun signUpWithGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignUpClient = GoogleSignIn.getClient(requireContext(), gso)
        googleSignUpClient.signOut().addOnCompleteListener {
            googleSignUpLauncher.launch(googleSignUpClient.signInIntent)
        }
    }
}