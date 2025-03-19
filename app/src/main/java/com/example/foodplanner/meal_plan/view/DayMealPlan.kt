package com.example.foodplanner.meal_plan.view

import com.example.foodplanner.core.model.remote.Meal

data class DayMealPlan(
    val dayName: String,
    val meals: MutableList<Meal> = mutableListOf()
)
