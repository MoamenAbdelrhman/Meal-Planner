package com.example.foodplanner.core.model.local.repository

import com.example.foodplanner.core.model.local.User
import com.example.foodplanner.core.model.local.source.UserDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser


interface UserRepository {

    suspend fun addUser(user: User)

    suspend fun getUserByEmail(email: String): User?

    suspend fun getUserByFirebaseUid(firebaseUid: String): User?

    suspend fun getLoggedInUser(): User?

    suspend fun deleteLoggedInUser()

    suspend fun updateUser(user: User)

    suspend fun deleteUser(email: String)

    suspend fun getPassword(email: String): String?

    suspend fun findLoggedInUser(): Boolean

    suspend fun logInUser(email: String)

    suspend fun logOutUser()

    suspend fun getLoggedInEmail(): String

    suspend fun getLoggedInUsername(): String

    suspend fun getCuisines(): List<String>

    suspend fun updateCuisines(cuisines: List<String>)

    suspend fun updateFavourites(favourites: List<String>)

    suspend fun updateSubscriptionState(isSubscribed: Boolean)

    suspend fun checkSubscriptionState(): Boolean

    fun getCurrentUser(): User?

    suspend fun saveUserToLocalDatabase(user: User)

}