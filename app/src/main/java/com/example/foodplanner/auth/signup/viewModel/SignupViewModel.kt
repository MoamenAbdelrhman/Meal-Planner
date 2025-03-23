package com.example.foodplanner.auth.signup.viewModel

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodplanner.core.model.local.User
import com.example.foodplanner.core.model.local.repository.UserRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class ValidationResult(
    val isValid: Boolean,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null
)

class SignupViewModel(private val userRepository: UserRepositoryImpl) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _signupSuccess = MutableLiveData<Boolean>()
    val signupSuccess: LiveData<Boolean> get() = _signupSuccess

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: MutableLiveData<String?> get() = _errorMessage

    private val _googleSignInSuccess = MutableLiveData<Boolean>()
    val googleSignInSuccess: LiveData<Boolean> get() = _googleSignInSuccess

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    // Validate user inputs for signup (name, email, password, confirm password)
    fun validateInputs(name: String, email: String, password: String, confirmPassword: String): ValidationResult {
        var nameError: String? = null
        var emailError: String? = null
        var passwordError: String? = null
        var confirmPasswordError: String? = null

        if (name.isEmpty()) {
            nameError = "Name is required"
        }
        if (email.isEmpty()) {
            emailError = "Email is required"
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Please enter a valid email"
        }

        passwordError = validatePassword(password)
        confirmPasswordError = validateConfirmPassword(password, confirmPassword)

        val isValid = nameError == null && emailError == null && passwordError == null && confirmPasswordError == null
        return ValidationResult(isValid, nameError, emailError, passwordError, confirmPasswordError)
    }

    // Validate password based on specific criteria (length, numbers, letters, special characters)
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

    // Validate that confirm password matches the password
    private fun validateConfirmPassword(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isEmpty() -> "Confirmation cannot be empty"
            confirmPassword != password -> "Passwords do not match"
            else -> null
        }
    }

    // Sign up a new user with email, password, and username using Firebase Auth
    fun signupUser(email: String, password: String, username: String) {
        _isLoading.value = true
        auth.fetchSignInMethodsForEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val result = task.result
                    val signInMethods = result?.signInMethods
                    if (signInMethods != null && signInMethods.isNotEmpty()) {
                        _errorMessage.value = "This email is already registered"
                        _isLoading.value = false
                    } else {
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { createTask ->
                                _isLoading.value = false
                                if (createTask.isSuccessful) {
                                    val firebaseUser = auth.currentUser
                                    if (firebaseUser != null) {
                                        val profileUpdates = UserProfileChangeRequest.Builder()
                                            .setDisplayName(username)
                                            .build()
                                        firebaseUser.updateProfile(profileUpdates)
                                            .addOnCompleteListener { profileTask ->
                                                if (profileTask.isSuccessful) {
                                                    saveUserToRoom(firebaseUser, username)
                                                } else {
                                                    _errorMessage.value = "Failed to update profile: ${profileTask.exception?.message}"
                                                }
                                            }
                                    }
                                } else {
                                    _errorMessage.value = "Signup failed: ${createTask.exception?.message}"
                                }
                            }
                    }
                } else {
                    _errorMessage.value = "Error checking sign-in methods"
                    _isLoading.value = false
                }
            }
    }

    // Sign in a user using Google credentials and save their info
    fun signInWithGoogle(idToken: String) {
        _isLoading.value = true
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        Log.d("SignupViewModel", "Google sign-in successful for user: ${firebaseUser.email}, UID: ${firebaseUser.uid}")
                        if (task.result?.additionalUserInfo?.isNewUser == true) {
                            Log.d("SignupViewModel", "New user created in Firebase")
                        } else {
                            Log.d("SignupViewModel", "Existing user signed in")
                        }
                        saveUserToRoom(firebaseUser, firebaseUser.displayName ?: "Unknown")
                    } else {
                        Log.e("SignupViewModel", "Google sign-in succeeded but firebaseUser is null")
                        _errorMessage.value = "Google login failed: User not found"
                    }
                } else {
                    Log.e("SignupViewModel", "Google sign-in failed: ${task.exception?.message}")
                    _errorMessage.value = "Google login failed: ${task.exception?.message}"
                }
            }
            .addOnFailureListener { exception ->
                Log.e("SignupViewModel", "Google sign-in failed with exception: ${exception.message}")
                _errorMessage.value = "Google login failed: ${exception.message}"
                _isLoading.value = false
            }
    }

    // Save the user's information to the local Room database
    private fun saveUserToRoom(firebaseUser: FirebaseUser, username: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingUser = userRepository.getUserByEmail(firebaseUser.email!!)
            if (existingUser == null) {
                val newUser = User(
                    id = firebaseUser.uid,
                    firebaseId = firebaseUser.uid,
                    email = firebaseUser.email!!,
                    password = "",
                    username = username
                )
                userRepository.addUser(newUser)
                Log.d("SignupViewModel", "User saved to Room: ${newUser.email}")
            } else {
                Log.d("SignupViewModel", "User already exists in Room: ${existingUser.email}")
            }
            _signupSuccess.postValue(true)
            _googleSignInSuccess.postValue(true)
        }
    }
}