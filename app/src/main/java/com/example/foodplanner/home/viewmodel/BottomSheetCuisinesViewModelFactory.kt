package com.example.foodplanner.home.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.foodplanner.core.model.local.repository.UserRepository
import com.example.foodplanner.core.model.remote.repository.MealRepository

class BottomSheetCuisinesViewModelFactory(

    private val userRepository: UserRepository,
    private val mealRepository: MealRepository

) : ViewModelProvider.Factory {


    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(BottomSheetCuisinesViewModel::class.java))
            return BottomSheetCuisinesViewModel(userRepository, mealRepository) as T
        else
            throw IllegalArgumentException("Unknown view model")

    }

}