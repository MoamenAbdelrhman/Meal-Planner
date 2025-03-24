package com.example.foodplanner.core.model.remote.source

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.foodplanner.core.model.entity.MealPlanEntity

@Dao
interface MealPlanDao {
    @Query("SELECT * FROM meal_plan WHERE userId = :userId")
    suspend fun getMealPlan(userId: String): List<MealPlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealPlan(mealPlan: List<MealPlanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSingleDayMealPlan(dayMeal: MealPlanEntity)

    @Query("DELETE FROM meal_plan WHERE userId = :userId")
    suspend fun clearMealPlan(userId: String)

    @Query("DELETE FROM meal_plan WHERE userId = :userId AND dayName = :dayName AND idMeal = :mealId")
    suspend fun deleteMealPlanForDayAndMeal(userId: String, dayName: String, mealId: String)
}