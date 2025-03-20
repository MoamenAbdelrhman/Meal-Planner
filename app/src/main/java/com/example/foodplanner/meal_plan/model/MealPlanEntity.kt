package com.example.foodplanner.meal_plan.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.example.foodplanner.core.model.remote.Meal
import com.example.foodplanner.meal_plan.view.DayMealPlan
import com.google.gson.Gson

@Entity(tableName = "meal_plan", primaryKeys = ["userId", "idMeal", "dayName"])
data class MealPlanEntity(
    @ColumnInfo(name = "userId") val userId: String,
    @ColumnInfo(name = "idMeal") val idMeal: String,
    @ColumnInfo(name = "dayName") val dayName: String,
    @ColumnInfo(name = "mealData") val mealData: String
)

fun MealPlanEntity.toMeal(): Meal {
    val gson = Gson()
    val meal = gson.fromJson(mealData, Meal::class.java)
    meal.populateIngredientsWithMeasures() // ملء القائمة بعد التحويل
    return meal
}

fun Meal.toMealPlanEntity(userId: String, dayName: String): MealPlanEntity {
    val gson = Gson()
    val mealData = gson.toJson(this)
    return MealPlanEntity(userId, idMeal, dayName, mealData)
}

fun List<MealPlanEntity>.toDayMealPlans(): List<DayMealPlan> {
    return this.groupBy { it.dayName }
        .map { (dayName, entities) ->
            val meals = entities.map { it.toMeal() }.toMutableList()
            DayMealPlan(dayName, meals)
        }
}