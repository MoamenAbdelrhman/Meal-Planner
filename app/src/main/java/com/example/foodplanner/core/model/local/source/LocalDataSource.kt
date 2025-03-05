package com.example.foodplanner.core.model.local.source

import com.example.foodplanner.core.model.local.User


interface LocalDataSource {
    suspend fun addUser(user: User)

    suspend fun getLoggedInUser(): User?

    suspend fun getUserByEmail(email: String): User?

    suspend fun getUserByFirebaseUid(firebaseUid: String): User?

    suspend fun deleteLoggedInUser()

    suspend fun getPassword(email: String): String?

    suspend fun findLoggedInUser(): Boolean

    suspend fun logInUser(email: String)

    suspend fun logOutUser()

    suspend fun getLoggedInEmail(): String

    suspend fun getLoggedInUsername(): String

    suspend fun getUserCuisines(): List<String>

    suspend fun updateUserCuisines(cuisines: List<String>)

    suspend fun updateUserFavourites(favourites: List<String>)

    suspend fun updateSubscriptionState(isSubscribed: Boolean)

    suspend fun checkSubscriptionState(): Boolean

    suspend fun deleteUserByEmail(email: String)
    suspend fun updateUser(user: User)
    suspend fun getPasswordByEmail(email: String): String?
    suspend fun insertUser(user: User)

}