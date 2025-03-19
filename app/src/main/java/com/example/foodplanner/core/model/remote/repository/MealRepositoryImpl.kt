package com.example.foodplanner.core.model.remote.repository

import com.example.foodplanner.core.model.remote.IngredientResponse
import com.example.foodplanner.core.model.remote.Meal
import com.example.foodplanner.core.model.remote.source.RemoteGsonData


class MealRepositoryImpl(

    private val remoteGsonDataSource: RemoteGsonData

) : MealRepository {


    override suspend fun getCategories() = remoteGsonDataSource.getRemoteGsonDataCategories()

    override suspend fun getRandomDataMeal() = remoteGsonDataSource.getRemoteGsonDataMeal()

    override suspend fun getCuisinesMeals(area: String) =
        remoteGsonDataSource.getCuisinesMeals(area)

    override suspend fun getCuisines() = remoteGsonDataSource.getCuisines()

    override suspend fun getMealsBySearch(title: String) =
        remoteGsonDataSource.getMealsBySearch(title)

    override suspend fun getCategoryMeals(category: String) =
        remoteGsonDataSource.getCategoryMeals(category)

    override suspend fun getMealById(id: String) = remoteGsonDataSource.getMealById(id)

    override suspend fun getAllCuisines(): List<String> {
        val cuisinesData = remoteGsonDataSource.getCuisines()
        return cuisinesData.meals.map { it.strArea }
    }

    override suspend fun getMealsByArea(area: String): List<Meal> {
        return remoteGsonDataSource.getCuisinesMeals(area).meals
    }

    override suspend fun getMealsByIngredient(ingredient: String): List<Meal> {
        return remoteGsonDataSource.getMealsByIngredient(ingredient).meals
    }

    override suspend fun getMealsByCategory(category: String): List<Meal> {
        return remoteGsonDataSource.getCategoryMeals(category).meals
    }
    override suspend fun getIngredients(): IngredientResponse = remoteGsonDataSource.getIngredients()


}