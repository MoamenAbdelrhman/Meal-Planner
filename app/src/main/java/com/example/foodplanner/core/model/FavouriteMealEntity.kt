package com.example.foodplanner.core.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.foodplanner.core.model.remote.Meal
import com.google.gson.Gson

@Entity(tableName = "user_favourites")
data class FavouriteMealEntity(
    @PrimaryKey @ColumnInfo(name = "idMeal") val idMeal: String,
    @ColumnInfo(name = "meal_data") val mealData: String,
    @ColumnInfo(name = "userId") val userId: String
)

fun FavouriteMealEntity.toMeal(): Meal {
    val gson = Gson()
    return gson.fromJson(mealData, Meal::class.java)
}

fun Meal.toFavouriteMealEntity(userId: String): FavouriteMealEntity {
    val gson = Gson()
    val mealData = gson.toJson(this)
    return FavouriteMealEntity(idMeal, mealData, userId)
}