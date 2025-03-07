package com.example.foodplanner.auth.signup.viewModel

import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseUser

class SignupViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _signupSuccess = MutableLiveData<Boolean>()
    val signupSuccess: LiveData<Boolean> get() = _signupSuccess

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private val _googleSignInSuccess = MutableLiveData<Boolean>()
    val googleSignInSuccess: LiveData<Boolean> get() = _googleSignInSuccess

    fun validateInputs(name: String, email: String, password: String, confirmPassword: String): Boolean {
        if (name.isEmpty()) {
            _errorMessage.value = "Name is required"
            return false
        }
        if (email.isEmpty()) {
            _errorMessage.value = "Email is required"
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _errorMessage.value = "Please enter a valid email"
            return false
        }

        val passwordValidationResult = validatePassword(password)
        if (passwordValidationResult != null) {
            _errorMessage.value = passwordValidationResult
            return false
        }

        val confirmPasswordValidationResult = validateConfirmPassword(password, confirmPassword)
        if (confirmPasswordValidationResult != null) {
            _errorMessage.value = confirmPasswordValidationResult
            return false
        }
        return true
    }

    private fun validatePassword(password: String): String? {
        return when {
            password.isEmpty() -> "Password cannot be empty"
            password.length < 8 -> "Minimum 8 characters long"
            !password.matches(".*[0-9].*".toRegex()) -> "Minimum one number"
            !password.matches(".*[A-Z].*".toRegex()) -> "Minimum one uppercase letter"
            !password.matches(".*[a-z].*".toRegex()) -> "Minimum one lowercase letter"
            !password.matches(".*[!@#$%^&*()_+].*".toRegex()) -> "Minimum one special character"
            else -> null
        }
    }

    private fun validateConfirmPassword(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isEmpty() -> "Confirmation cannot be empty"
            confirmPassword != password -> "Passwords do not match"
            else -> null
        }
    }

    fun signupUser(email: String, password: String) {
        auth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val result = task.result
                    val signInMethods = result?.signInMethods

                    if (signInMethods != null && signInMethods.isNotEmpty()) {
                        _errorMessage.value = "This email is already registered"
                    } else {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    _signupSuccess.value = true
                                } else {
                                    _errorMessage.value = "Signup failed: ${task.exception?.message}"
                                }
                            }
                    }
                } else {
                    _errorMessage.value = "Error checking sign-in methods"
                }
            }
    }

    fun signInWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _googleSignInSuccess.value = true
                } else {
                    _errorMessage.value = "Google login failed"
                }
            }
    }
}