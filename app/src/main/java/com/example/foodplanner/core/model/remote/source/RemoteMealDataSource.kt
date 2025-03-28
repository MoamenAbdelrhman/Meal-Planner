package com.example.foodplanner.core.model.remote.source

import com.example.foodplanner.core.model.remote.GsonDataArea
import com.example.foodplanner.core.model.remote.GsonDataCategories
import com.example.foodplanner.core.model.remote.GsonDataMeal
import com.example.foodplanner.core.model.remote.IngredientResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface RemoteMealDataSource {
    @GET("categories.php")
    suspend fun getAllCategories(): GsonDataCategories

    @GET("random.php")
    suspend fun getRandomMeal(): GsonDataMeal

    @GET("filter.php")
    suspend fun getCuisinesMeals(@Query("a") area: String): GsonDataMeal

    @GET("search.php")
    suspend fun getMealsBySearch(@Query("s") title: String): GsonDataMeal

    @GET("list.php?a=list")
    suspend fun getCuisines(): GsonDataArea

    @GET("filter.php")
    suspend fun getCategoryMeals(@Query("c") category: String): GsonDataMeal

    @GET("lookup.php")
    suspend fun getMealById(@Query("i") id: String): GsonDataMeal

    @GET("filter.php")
    suspend fun getMealsByIngredient(@Query("i") ingredient: String): GsonDataMeal

    @GET("list.php?i=list")
    suspend fun getIngredients(): IngredientResponse

}