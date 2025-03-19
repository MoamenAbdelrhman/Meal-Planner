package com.example.foodplanner.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.foodplanner.core.model.FavouriteMealDao
import com.example.foodplanner.core.model.local.repository.UserRepository
import com.example.foodplanner.core.model.remote.repository.MealRepository

class DataViewModelFactory(
    private val userRepository: UserRepository,
    private val mealRepository: MealRepository,
    private val favouriteMealDao: FavouriteMealDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DataViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DataViewModel(userRepository, mealRepository, favouriteMealDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}