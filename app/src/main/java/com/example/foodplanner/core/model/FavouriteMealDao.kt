package com.example.foodplanner.core.model

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FavouriteMealDao {
    @Query("SELECT * FROM user_favourites WHERE userId = :userId")
    suspend fun getAllFavouriteMeals(userId: String): List<FavouriteMealEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavouriteMeal(meal: FavouriteMealEntity)

    @Query("DELETE FROM user_favourites WHERE userId = :userId AND idMeal = :idMeal")
    suspend fun deleteFavouriteMeal(idMeal: String, userId: String)

    @Query("DELETE FROM user_favourites WHERE userId = :userId")
    suspend fun clearFavouriteMeals(userId: String)
}