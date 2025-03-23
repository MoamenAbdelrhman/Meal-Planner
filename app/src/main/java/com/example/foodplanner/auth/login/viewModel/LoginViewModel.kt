package com.example.foodplanner.auth.login.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodplanner.core.model.local.User
import com.example.foodplanner.core.model.local.repository.UserRepositoryImpl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LoginViewModel(
    private val userRepository: UserRepositoryImpl
) : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loginUser(email: String, password: String) {
        _isLoading.value = true

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        saveUserToRoom(firebaseUser)
                    }
                    _loginSuccess.value = true // إطلاق loginSuccess مرة واحدة فقط هنا
                } else {
                    handleLoginError(task.exception)
                }
            }
    }

    fun firebaseAuthWithGoogle(idToken: String) {
        _isLoading.value = true
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                _isLoading.value = false
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        saveUserToRoom(firebaseUser)
                    }
                    _loginSuccess.value = true // إطلاق loginSuccess مرة واحدة فقط هنا
                } else {
                    _errorMessage.value = "Google sign-in failed"
                    Log.e("LoginViewModel", "Google sign-in failed: ${task.exception?.message}")
                }
            }
    }

    private fun saveUserToRoom(firebaseUser: FirebaseUser) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingUser = userRepository.getUserByEmail(firebaseUser.email!!)
            if (existingUser == null) {
                val newUser = User(
                    id = "0", // Auto-increment
                    firebaseId = firebaseUser.uid,
                    email = firebaseUser.email!!,
                    password = "",
                    username = firebaseUser.displayName ?: "Unknown"
                )
                userRepository.addUser(newUser)
            }
            // لا نستخدم postValue هنا لأن loginSuccess يتم إطلاقه بالفعل في الخيط الرئيسي
        }
    }

    private fun handleLoginError(exception: Exception?) {
        Log.d("LoginViewModel", "Login error: ${exception?.message}")
        _errorMessage.value = when (exception) {
            is FirebaseAuthInvalidUserException -> "Email not registered"
            is FirebaseAuthInvalidCredentialsException -> "Invalid password"
            else -> exception?.message ?: "Authentication failed"
        }
    }
}