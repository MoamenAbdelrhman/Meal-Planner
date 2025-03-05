package com.example.foodplanner.core.model.local.source

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.foodplanner.core.model.UserEntity
import com.example.foodplanner.core.model.local.User

@Dao
interface UserDao {

    /**
     * Inserts a new user into the database.
     * If the user already exists, it replaces the old entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    /**
     * Retrieves a user by their email.
     */
    @Query("SELECT * FROM user WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    /**
     * Retrieves a user by their Firebase UID.
     */
    @Query("SELECT * FROM user WHERE firebaseId = :firebaseUid LIMIT 1")
    suspend fun getUserByFirebaseUid(firebaseUid: String): User?

    /**
     * Updates user information.
     */
    @Update
    suspend fun updateUser(user: User)

    /**
     * Deletes a user by email.
     */
    @Query("DELETE FROM user WHERE email = :email")
    suspend fun deleteUserByEmail(email: String)

    /**
     * Retrieves the stored password for a given email.
     */
    @Query("SELECT password FROM user WHERE email = :email LIMIT 1")
    suspend fun getPasswordByEmail(email: String): String?

    /**
     * Deletes all users from the database.
     */
    @Query("DELETE FROM user")
    suspend fun deleteAllUsers()


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)

    /*@Query("SELECT * FROM user WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM user WHERE firebaseId = :firebaseUid LIMIT 1")
    suspend fun getUserByFirebaseUid(firebaseUid: String): User?*/


    @Query("SELECT * FROM user WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getLoggedInUser(): User?

    @Delete
    suspend fun deleteUser(user: User)

    @Query("DELETE FROM user WHERE isLoggedIn = 1")
    suspend fun deleteLoggedInUser()

    @Query("SELECT password FROM user WHERE email = :email LIMIT 1")
    suspend fun getPassword(email: String): String?

    @Query("SELECT isLoggedIn FROM user WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun findLoggedInUser(): Boolean

    @Query("UPDATE user SET isLoggedIn = 1 WHERE email = :email")
    suspend fun logInUser(email: String)

    @Query("UPDATE user SET isLoggedIn = 0 WHERE isLoggedIn = 1")
    suspend fun logOutUser()

    @Query("SELECT email FROM user WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getLoggedInEmail(): String

    @Query("SELECT username FROM user WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getLoggedInUsername(): String

    @Query("SELECT cuisines FROM user WHERE isLoggedIn = 1")
    suspend fun getLoggedInUserCuisines(): List<String>

    @Query("UPDATE user SET cuisines = :cuisines WHERE isLoggedIn = 1")
    suspend fun updateLoggedInUserCuisines(cuisines: List<String>)

    @Query("UPDATE user SET favourites = :favourites WHERE isLoggedIn = 1")
    suspend fun updateLoggedInUserFavourites(favourites: List<String>)

    @Query("UPDATE user SET isSubscribed = :subscribed WHERE isLoggedIn = 1")
    suspend fun updateSubscriptionState(subscribed: Boolean)

    @Query("SELECT isSubscribed FROM user WHERE isLoggedIn = 1")
    suspend fun checkSubscriptionState(): Boolean

}