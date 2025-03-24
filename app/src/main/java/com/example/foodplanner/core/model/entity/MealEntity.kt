package com.example.foodplanner.core.model.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favourite_meals")
data class MealEntity(
    @PrimaryKey val idMeal: String,
    val strMeal: String,
    val strMealThumb: String? = null,
    val userId: String
)