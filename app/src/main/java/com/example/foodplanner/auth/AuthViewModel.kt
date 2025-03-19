package com.example.foodplanner.auth

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodplanner.core.model.local.User
import com.example.foodplanner.core.model.local.repository.UserRepository
import com.example.foodplanner.core.model.remote.FailureReason
import com.example.foodplanner.core.model.remote.Response
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class AuthViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _user = MutableLiveData<User?>()
    val user: MutableLiveData<User?> get() = _user

    private val _authState = MutableLiveData<FirebaseUser?>()
    val authState: LiveData<FirebaseUser?> get() = _authState

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

    private val _loginStatus = MutableLiveData<Boolean>()
    val loginStatus: LiveData<Boolean> get() = _loginStatus

    init {
        Log.d("AuthDebug", "AuthViewModel Initialized - userRepository: $userRepository")
        user.value = userRepository.getCurrentUser()
        Log.d("AuthDebug", "Initial user value: ${user.value}")
    }

    private val _loggedOut = MutableLiveData<Response<Unit>>()
    val loggedOut: LiveData<Response<Unit>> get() = _loggedOut

    fun logOut() = applyResponse(_loggedOut) {
        userRepository.logOutUser()
    }

    fun saveUserToLocalDatabase(user: User) {
        viewModelScope.launch {
            userRepository.saveUserToLocalDatabase(user)
        }
    }

    private val _deletedAccount = MutableLiveData<Response<Unit>>()
    val deletedAccount: LiveData<Response<Unit>> get() = _deletedAccount

    fun deleteAccount() = applyResponse(_deletedAccount) {
        userRepository.deleteLoggedInUser()
    }

    private fun <T> applyResponse(
        liveData: MutableLiveData<Response<T>>,
        dataFetch: suspend () -> T
    ) {
        liveData.value = Response.Loading

        viewModelScope.launch {
            try {
                val result = dataFetch()
                liveData.value = Response.Success(result)
            } catch (e: IOException) {
                liveData.value = Response.Failure(FailureReason.NoInternet)
            } catch (e: Exception) {
                liveData.value = Response.Failure(
                    FailureReason.UnknownError(
                        error = e.message ?: "Unknown error occurred"
                    )
                )
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = firebaseAuth.currentUser
                        if (firebaseUser != null) {
                            viewModelScope.launch {
                                userRepository.logInUser(email)
                                _authState.value = firebaseUser
                                _loginStatus.value = true
                            }
                        }
                    } else {
                        _error.value = task.exception?.message ?: "Login failed"
                        _loginStatus.value = false
                    }
                }
        }
    }

    fun signUp(username: String, email: String, password: String) {
        viewModelScope.launch {
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = firebaseAuth.currentUser
                        if (firebaseUser != null) {
                            val newUser = User(
                                username = username,
                                firebaseId = firebaseUser.uid,
                                email = email,
                                password = password,
                                isLoggedIn = true
                            )
                            viewModelScope.launch {
                                userRepository.addUser(newUser)
                                _authState.value = firebaseUser
                            }
                        }
                    } else {
                        _error.value = task.exception?.message ?: "Registration failed"
                    }
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            firebaseAuth.signOut()
            userRepository.logOutUser()
            _authState.postValue(null)
            _loginStatus.postValue(false)
            _user.postValue(null)
        }
    }

    fun checkUserLoggedIn() {
        _authState.postValue(firebaseAuth.currentUser)
        _loginStatus.postValue(firebaseAuth.currentUser != null)
    }

    fun getLoggedInEmail(): LiveData<String> {
        val emailLiveData = MutableLiveData<String>()
        viewModelScope.launch {
            val email = userRepository.getLoggedInEmail()
            emailLiveData.postValue(email)
        }
        return emailLiveData
    }
}