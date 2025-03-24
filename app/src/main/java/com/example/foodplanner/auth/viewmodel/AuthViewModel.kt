package com.example.foodplanner.auth.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodplanner.core.model.local.User
import com.example.foodplanner.core.model.local.repository.UserRepository
import com.example.foodplanner.core.model.remote.FailureReason
import com.example.foodplanner.core.model.remote.Response
import kotlinx.coroutines.launch
import java.io.IOException

class AuthViewModel(private val userRepository: UserRepository) : ViewModel() {

    private val _user = MutableLiveData<User?>()
    val user: MutableLiveData<User?> get() = _user

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> get() = _error

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
}