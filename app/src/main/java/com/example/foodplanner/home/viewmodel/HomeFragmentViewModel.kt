package com.example.foodplanner.home.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodplanner.core.model.local.repository.UserRepository
import com.example.foodplanner.core.model.remote.FailureReason
import com.example.foodplanner.core.model.remote.GsonDataCategories
import com.example.foodplanner.core.model.remote.GsonDataMeal
import com.example.foodplanner.core.model.remote.Meal
import com.example.foodplanner.core.model.remote.Response
import com.example.foodplanner.core.model.remote.repository.MealRepository
import com.example.foodplanner.utils.NetworkUtils
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class HomeFragmentViewModel(
    private val mealRepository: MealRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private var _dataCategories: MutableLiveData<Response<GsonDataCategories>> = MutableLiveData()
    val dataCategories: LiveData<Response<GsonDataCategories>> get() = _dataCategories

    // Fetch meal categories from the repository
    fun getCategories(context: android.content.Context) {
        applyResponse(_dataCategories, context) {
            mealRepository.getCategories()
        }
    }

    private val _defaultMeals = MutableLiveData<Response<MutableList<Meal>>>()
    val defaultMeals: LiveData<Response<MutableList<Meal>>> get() = _defaultMeals

    // Fetch a specified number of default meals asynchronously
    fun getDefaultMeals(much: Int, context: android.content.Context) {
        _defaultMeals.value = Response.Loading

        val handler = CoroutineExceptionHandler { _, exception ->
            _defaultMeals.value = Response.Failure(
                FailureReason.UnknownError(
                    error = exception.message ?: "Unknown error occurred"
                )
            )
            Log.e("HomeFragmentViewModel", "Unknown: ${exception.message}")
        }

        viewModelScope.launch(handler) {
            if (NetworkUtils.isInternetAvailable(context)) {
                val foundMeals = mutableListOf<Deferred<Meal>>().apply {
                    repeat(much) {
                        add(
                            async {
                                mealRepository.getRandomDataMeal().meals.first()
                            }
                        )
                    }
                }
                val meals = foundMeals.awaitAll().toMutableList()
                _defaultMeals.value = Response.Success(meals)
            } else {
                _defaultMeals.value = Response.Failure(FailureReason.NoInternet)
            }
        }
    }

    private var _filteredMealsByCategory: MutableLiveData<Response<GsonDataMeal>> = MutableLiveData()
    val filteredMealsByCategory: LiveData<Response<GsonDataMeal>> get() = _filteredMealsByCategory

    // Fetch meals filtered by a specific category
    fun getFilteredMealsByCategory(category: String, context: android.content.Context) {
        applyResponse(_filteredMealsByCategory, context) {
            mealRepository.getCategoryMeals(category)
        }
    }

    private val _userCuisines: MutableLiveData<Response<List<String>?>> = MutableLiveData()
    val userCuisines: LiveData<Response<List<String>?>> get() = _userCuisines

    // Fetch the logged-in user's preferred cuisines
    fun getUserCuisines(context: android.content.Context) {
        applyResponse(_userCuisines, context) {
            userRepository.getLoggedInUser()?.cuisines
        }
    }

    private var _filteredMealsByAreas: MutableLiveData<Response<GsonDataMeal>> = MutableLiveData()
    val filteredMealsByAreas: LiveData<Response<GsonDataMeal>> get() = _filteredMealsByAreas

    // Fetch meals filtered by a specific area/cuisine
    fun getFilteredMealsByAreas(area: String, context: android.content.Context) {
        applyResponse(_filteredMealsByAreas, context) {
            mealRepository.getCuisinesMeals(area)
        }
    }

    private val _randomMeal = MutableLiveData<Response<Meal>>()
    val randomMeal: LiveData<Response<Meal>> get() = _randomMeal

    // Fetch a random meal from the repository
    fun getRandomMeal(context: android.content.Context) {
        applyResponse(_randomMeal, context) {
            mealRepository.getRandomDataMeal().meals.first()
        }
    }

    private val _allCuisines = MutableLiveData<Response<List<String>>>()
    val allCuisines: LiveData<Response<List<String>>> get() = _allCuisines

    // Fetch all available cuisines from the repository
    fun getAllCuisines(context: android.content.Context) = applyResponse(_allCuisines, context) {
        mealRepository.getAllCuisines()
    }

    private val _someGoldMeals = MutableLiveData<Response<MutableList<Meal>>>()
    val someGoldMeals: LiveData<Response<MutableList<Meal>>> get() = _someGoldMeals

    private val _someRecommendedMeals = MutableLiveData<Response<MutableList<Meal>>>()
    val someRecommendedMeals: LiveData<Response<MutableList<Meal>>> get() = _someRecommendedMeals

    // Fetch a specified number of random meals (for gold or recommended sections)
    fun getRandomMeals(much: Int, isGold: Boolean = false, context: android.content.Context) {
        val responseHandler = MutableLiveData<Response<MutableList<Meal>>>()
        responseHandler.observeForever { response ->
            when (isGold) {
                true -> _someGoldMeals.value = response
                false -> _someRecommendedMeals.value = response
            }
        }
        responseHandler.value = Response.Loading

        val handler = CoroutineExceptionHandler { _, exception ->
            responseHandler.value = Response.Failure(
                FailureReason.UnknownError(
                    error = exception.message ?: "Unknown error occurred"
                )
            )
            Log.e("HomeFragmentViewModel", "Unknown: ${exception.message}")
        }

        viewModelScope.launch(handler) {
            if (NetworkUtils.isInternetAvailable(context)) {
                val foundMeals = mutableListOf<Deferred<Meal>>().apply {
                    repeat(much) {
                        add(
                            async {
                                mealRepository.getRandomDataMeal().meals.first()
                            }
                        )
                    }
                }
                val meals = foundMeals.awaitAll().toMutableList()
                responseHandler.value = Response.Success(meals)
            } else {
                responseHandler.value = Response.Failure(FailureReason.NoInternet)
            }
            responseHandler.removeObserver { }
        }
    }

    // Fetch a random category and pass it to the callback
    fun getRandomCategory(onResult: (String) -> Unit, context: android.content.Context) {
        viewModelScope.launch {
            if (NetworkUtils.isInternetAvailable(context)) {
                try {
                    val categoriesResponse = mealRepository.getCategories()
                    val randomCategory = categoriesResponse.categories.random().strCategory
                    onResult(randomCategory)
                } catch (e: Exception) {
                    Log.e("HomeFragmentViewModel", "Failed to fetch random category: ${e.message}")
                    onResult("Beef")
                }
            } else {
                onResult("Beef") // Default value when internet is unavailable
            }
        }
    }

    // Fetch a random cuisine and pass it to the callback
    fun getRandomCuisine(onResult: (String) -> Unit, context: android.content.Context) {
        viewModelScope.launch {
            if (NetworkUtils.isInternetAvailable(context)) {
                try {
                    val cuisinesResponse = mealRepository.getAllCuisines()
                    val randomCuisine = cuisinesResponse.random()
                    onResult(randomCuisine)
                } catch (e: Exception) {
                    Log.e("HomeFragmentViewModel", "Failed to fetch random cuisine: ${e.message}")
                    onResult("Egyptian")
                }
            } else {
                onResult("Egyptian") // Default value when internet is unavailable
            }
        }
    }

    // Generic function to handle API responses with loading, success, and failure states
    private fun <T> applyResponse(
        liveData: MutableLiveData<Response<T>>,
        context: android.content.Context,
        dataFetch: suspend () -> T
    ) {
        liveData.value = Response.Loading

        viewModelScope.launch {
            if (NetworkUtils.isInternetAvailable(context)) {
                try {
                    val result = dataFetch()
                    liveData.value = Response.Success(result)
                } catch (e: IOException) {
                    liveData.value = Response.Failure(FailureReason.NoInternet)
                } catch (e: HttpException) {
                    liveData.value = Response.Failure(FailureReason.UnknownError(e.message()))
                }
            } else {
                liveData.value = Response.Failure(FailureReason.NoInternet)
            }
        }
    }
}