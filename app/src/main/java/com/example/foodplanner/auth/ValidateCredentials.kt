package com.example.foodplanner.auth

sealed class ValidateCredentials {
    object Valid : ValidateCredentials()
    data class InValid(val message: String) : ValidateCredentials()
}