package com.example.foodplanner.core.model.remote.source

import com.example.foodplanner.core.model.remote.GsonDataArea
import com.example.foodplanner.core.model.remote.GsonDataCategories
import com.example.foodplanner.core.model.remote.GsonDataMeal
import com.example.foodplanner.core.model.remote.Meal


interface RemoteGsonData {

    suspend fun getRemoteGsonDataCategories(): GsonDataCategories

    suspend fun getRemoteGsonDataMeal(): GsonDataMeal

    suspend fun getCuisinesMeals(area: String): GsonDataMeal

    suspend fun getCuisines(): GsonDataArea

    suspend fun getMealsBySearch(title: String): GsonDataMeal

    suspend fun getCategoryMeals(category: String): GsonDataMeal

    suspend fun getMealById(id: String): Meal

}