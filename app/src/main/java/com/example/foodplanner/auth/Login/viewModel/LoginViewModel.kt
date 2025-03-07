package com.example.foodplanner.auth.Login.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodplanner.auth.ValidateCredentials
import com.example.foodplanner.core.model.local.User
import com.example.foodplanner.core.model.local.repository.UserRepositoryImpl
import com.example.foodplanner.core.util.PasswordUtil
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

        viewModelScope.launch(Dispatchers.IO) {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    _isLoading.value = false
                    if (task.isSuccessful) {
                        val firebaseUser = auth.currentUser
                        _loginSuccess.value = true
                        if (firebaseUser != null) {
                            saveUserToRoom(firebaseUser)
                        }
                    } else {
                        handleLoginError(task.exception)
                    }
                }
        }
    }


    /** HANDLE FIREBASE GOOGLE AUTHENTICATION **/
    fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        saveUserToRoom(firebaseUser)
                    }
                } else {
                    _errorMessage.value = "Google sign-in failed"
                }
            }
    }

    /** SAVE AUTHENTICATED USER TO ROOM DATABASE **/
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
            _loginSuccess.postValue(true)
        }
    }

    /** HANDLE FIREBASE LOGIN ERRORS **/
    private fun handleLoginError(exception: Exception?) {
        _errorMessage.value = when (exception) {
            is FirebaseAuthInvalidUserException -> "Email not registered"
            is FirebaseAuthInvalidCredentialsException -> "Invalid password"
            else -> exception?.message ?: "Authentication failed"
        }
    }

}