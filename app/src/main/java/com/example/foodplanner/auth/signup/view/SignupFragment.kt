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
import com.example.foodplanner.main.view.MainActivity
import com.example.foodplanner.R
import com.example.foodplanner.auth.AuthActivity
import com.example.foodplanner.auth.signup.viewModel.SignupViewModel
import com.example.foodplanner.auth.signup.viewModel.SignupViewModelFactory
import com.example.foodplanner.core.model.local.repository.UserRepositoryImpl
import com.example.foodplanner.core.model.local.source.LocalDataSourceImpl
import com.example.foodplanner.core.model.local.source.UserDatabase
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
    private lateinit var signupButton: Button
    private lateinit var loginButton: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorTextView: TextView

    private var isValidationStarted = false // متغير لتتبع ما إذا بدأ التحقق

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    Log.d("SignupFragment", "Google account retrieved: ${account.email}")
                    viewModel.signInWithGoogle(account.idToken!!)
                } else {
                    Log.e("SignupFragment", "Google account is null")
                    Toast.makeText(context, "Google sign-in failed: No account", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Log.e("SignupFragment", "Google sign-in failed: ${e.statusCode}, ${e.message}")
                Toast.makeText(context, "Google sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.w("SignupFragment", "Google sign-in result not OK: ${result.resultCode}")
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
        signupButton = view.findViewById(R.id.signupButton)
        loginButton = view.findViewById(R.id.loginButton)
        progressBar = view.findViewById(R.id.progressBar)
        errorTextView = view.findViewById(R.id.errorTextView)

        // إعداد TextWatcher لتحديث رسائل الخطأ أثناء الكتابة
        setupTextWatchers()

        // Signup Button Click
        signupButton.setOnClickListener {
            isValidationStarted = true // بدء التحقق عند الضغط الأول

            val name = usernameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            // إعادة تعيين الأخطاء
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
            (activity as AuthActivity).navigateToLogin()
        }

        // Google Sign-Up Button Click
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.googleSignUpButton).setOnClickListener {
            signInWithGoogle()
        }

        // Observe ViewModel
        viewModel.signupSuccess.observe(viewLifecycleOwner, Observer { success ->
            if (success) {
                Toast.makeText(requireActivity(), "Signup successful!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireActivity(), MainActivity::class.java))
                requireActivity().finish()
            }
        })

        viewModel.errorMessage.observe(viewLifecycleOwner, Observer { error ->
            error?.let {
                Log.d("SignupFragment", "Error received: $it")
                errorTextView.text = it // عرض أخطاء Firebase في TextView
            }
        })

        viewModel.googleSignInSuccess.observe(viewLifecycleOwner, Observer { success ->
            if (success) {
                Toast.makeText(context, "Google login successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(requireContext(), MainActivity::class.java))
                requireActivity().finish()
            }
        })

        viewModel.isLoading.observe(viewLifecycleOwner, Observer { loading ->
            progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        })
    }

    private fun setupTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                // تحديث الرسائل فقط إذا كان التحقق قد بدأ (أي بعد الضغط على "Sign up")
                if (isValidationStarted) {
                    val name = usernameEditText.text.toString().trim()
                    val email = emailEditText.text.toString().trim()
                    val password = passwordEditText.text.toString().trim()
                    val confirmPassword = confirmPasswordEditText.text.toString().trim()

                    // إعادة التحقق من صحة المدخلات
                    val validationResult = viewModel.validateInputs(name, email, password, confirmPassword)

                    // تحديث رسائل الخطأ بناءً على النتيجة
                    usernameInputLayout.error = validationResult.nameError
                    emailInputLayout.error = validationResult.emailError
                    passwordInputLayout.error = validationResult.passwordError
                    confirmPasswordInputLayout.error = validationResult.confirmPasswordError
                }
            }
        }

        // إضافة TextWatcher لكل حقل
        usernameEditText.addTextChangedListener(textWatcher)
        emailEditText.addTextChangedListener(textWatcher)
        passwordEditText.addTextChangedListener(textWatcher)
        confirmPasswordEditText.addTextChangedListener(textWatcher)
    }

    private fun signInWithGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
        googleSignInClient.signOut().addOnCompleteListener {
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }
}