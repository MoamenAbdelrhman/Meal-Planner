package com.example.foodplanner.core.model.local.repository

import com.example.foodplanner.core.model.local.User
import com.example.foodplanner.core.model.local.source.LocalDataSource
import com.example.foodplanner.core.model.local.source.LocalDataSourceImpl
import com.example.foodplanner.core.model.local.source.UserDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepositoryImpl(
    private val userDao: LocalDataSource,
    private val auth: FirebaseAuth)
    : UserRepository {

    /**
     * Adds a new user to the local database.
     */
    override suspend fun addUser(user: User) {
        userDao.insertUser(user)
    }

    /**
     * Retrieves a user by their email from the local database.
     */
    override suspend fun getUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)
    }

    /**
     * Retrieves a user by their Firebase UID.
     */
    override suspend fun getUserByFirebaseUid(firebaseUid: String): User? {
        return userDao.getUserByFirebaseUid(firebaseUid)
    }

    /**
     * Returns the currently logged-in user from the local database.
     */
    override suspend fun getLoggedInUser(): User? {
        val email = getLoggedInEmail()
        return getUserByEmail(email)
    }

    /**
     * Deletes the currently logged-in user from the local database.
     */
    override suspend fun deleteLoggedInUser() {
        withContext(Dispatchers.IO) {
            val email = getLoggedInEmail()
            userDao.deleteUserByEmail(email)
        }
    }

    /**
     * Updates user information in the local database.
     */
    override suspend fun updateUser(user: User) {
        userDao.updateUser(user)
    }

    /**
     * Deletes a user by email from the local database.
     */
    override suspend fun deleteUser(email: String) {
        userDao.deleteUserByEmail(email)
    }

    /**
     * Retrieves the stored password for a given email from the local database.
     */
    override suspend fun getPassword(email: String): String? {
        return userDao.getPasswordByEmail(email)
    }

    /**
     * Checks if a user is currently logged in using Firebase authentication.
     */
    override suspend fun findLoggedInUser(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Logs in a user using Firebase authentication.
     */
    override suspend fun logInUser(email: String) {
        withContext(Dispatchers.IO) {
            val user = getUserByEmail(email)
            user?.let {
                auth.signInWithEmailAndPassword(it.email.toString(), getPassword(email) ?: "")
            }
        }
    }

    /**
     * Logs out the currently authenticated user from Firebase.
     */
    override suspend fun logOutUser() {
        auth.signOut()
    }

    /**
     * Retrieves the email of the currently logged-in user from Firebase.
     */
    override suspend fun getLoggedInEmail(): String {
        return auth.currentUser?.email ?: ""
    }

    /**
     * Retrieves the username of the currently logged-in user.
     */
    override suspend fun getLoggedInUsername(): String {
        return getLoggedInUser()?.username ?:""
    }

    /**
     * Retrieves the list of cuisines preferred by the logged-in user.
     */
    override suspend fun getCuisines(): List<String> {
        return getLoggedInUser()?.cuisines ?: emptyList()
    }

    /**
     * Updates the user's preferred cuisines.
     */
    override suspend fun updateCuisines(cuisines: List<String>) {
        getLoggedInUser()?.let {
            val updatedUser = it.copy(cuisines = cuisines)
            updateUser(updatedUser)
        }
    }

    /**
     * Updates the user's list of favorite meals.
     */
    override suspend fun updateFavourites(favourites: List<String>) {
        getLoggedInUser()?.let {
            val updatedUser = it.copy(favourites = favourites)
            updateUser(updatedUser)
        }
    }

    /**
     * Updates the user's subscription status.
     */
    override suspend fun updateSubscriptionState(isSubscribed: Boolean) {
        getLoggedInUser()?.let {
            val updatedUser = it.copy(isSubscribed = isSubscribed)
            updateUser(updatedUser)
        }
    }

    /**
     * Checks if the user has an active subscription.
     */
    override suspend fun checkSubscriptionState(): Boolean {
        return getLoggedInUser()?.isSubscribed ?: false
    }

    /**
     * Returns the currently authenticated Firebase user.
     */
    override fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * Saves user data to the local database.
     */
    override suspend fun saveUserToLocalDatabase(user: User) {
        userDao.addUser(user)
    }
}