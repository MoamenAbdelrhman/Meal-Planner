package com.example.foodplanner.meal_plan.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.foodplanner.core.model.local.repository.UserRepository
import com.example.foodplanner.core.model.local.source.UserDatabase
import com.example.foodplanner.core.model.remote.Meal
import com.example.foodplanner.meal_plan.model.MealPlanDao
import com.example.foodplanner.meal_plan.model.toDayMealPlans
import com.example.foodplanner.meal_plan.model.toMealPlanEntity
import com.example.foodplanner.meal_plan.view.DayMealPlan
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MealPlanViewModel(
    private val userRepository: UserRepository,
    context: Context
) : ViewModel() {

    private val mealPlanDao: MealPlanDao = UserDatabase.getDatabaseInstance(context).mealPlanDao()
    private var userId: String? = null

    private val _weeklyMealPlan = MutableLiveData<List<DayMealPlan>>()
    val weeklyMealPlan: LiveData<List<DayMealPlan>> get() = _weeklyMealPlan

    private val _isLoading = MutableLiveData<Boolean>(true)
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _clearMealPlanResult = MutableLiveData<Boolean>()
    val clearMealPlanResult: LiveData<Boolean> get() = _clearMealPlanResult

    private val mealPlanListeners = mutableMapOf<String, ChildEventListener>()

    companion object {
        private val DAYS_OF_WEEK = listOf(
            "Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"
        )
    }

    init {

        viewModelScope.launch {
            userId = userRepository.getCurrentUserId()
            if (userId == null) {
                Log.e("MealPlanViewModel", "User ID is null, cannot load meal plan")
                _weeklyMealPlan.value = emptyList()
                _isLoading.value = false
            } else {
                loadMealPlanFromDatabase()
                startMealPlansSync(userId!!)
            }
        }
    }

    // Start real-time synchronization with Firebase for each day of the week
    fun startMealPlansSync(userId: String) {
        this.userId = userId
        DAYS_OF_WEEK.forEach { day ->
            val dayRef = FirebaseDatabase.getInstance().getReference("/users/$userId/mealPlans/$day")
            val listener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val meal = snapshot.getValue(Meal::class.java)
                    meal?.let {
                        viewModelScope.launch {
                            // Insert the new meal into Room and refresh the UI
                            mealPlanDao.insertSingleDayMealPlan(it.toMealPlanEntity(userId, day))
                            loadMealPlanFromDatabase()
                        }
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val mealId = snapshot.key
                    mealId?.let {
                        viewModelScope.launch {
                            // Remove the meal from Room and refresh the UI
                            mealPlanDao.deleteMealPlanForDayAndMeal(userId, day, it)
                            loadMealPlanFromDatabase()
                        }
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val meal = snapshot.getValue(Meal::class.java)
                    meal?.let {
                        it.populateIngredientsWithMeasures() // Populate ingredients after deserialization
                        viewModelScope.launch {
                            // Update the meal in Room and refresh the UI
                            mealPlanDao.insertSingleDayMealPlan(it.toMealPlanEntity(userId, day))
                            loadMealPlanFromDatabase()
                        }
                    }
                }
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {
                    Log.e("MealPlanViewModel", "Failed to sync $day plan", error.toException())
                }
            }
            dayRef.addChildEventListener(listener)
            mealPlanListeners[day] = listener
        }
    }

    // Stop real-time synchronization by removing all Firebase listeners
    fun stopMealPlansSync() {
        userId?.let { uid ->
            DAYS_OF_WEEK.forEach { day ->
                val dayRef = FirebaseDatabase.getInstance().getReference("/users/$uid/mealPlans/$day")
                mealPlanListeners[day]?.let { dayRef.removeEventListener(it) }
            }
            mealPlanListeners.clear()
        }
    }

    // Load the meal plan from Room database and update the LiveData
    private fun loadMealPlanFromDatabase() {
        viewModelScope.launch {
            userId?.let { id ->
                val savedPlanEntities = mealPlanDao.getMealPlan(id)
                val savedPlan = savedPlanEntities.toDayMealPlans()
                // Ensure all days are included, even if empty
                val fullWeekPlan = DAYS_OF_WEEK.map { dayName ->
                    savedPlan.find { it.dayName.equals(dayName, ignoreCase = true) } ?: DayMealPlan(dayName)
                }
                _weeklyMealPlan.value = fullWeekPlan
                _isLoading.value = false
            }
        }
    }

    // Add a meal to the specified day and sync with Firebase and Room
    fun addMealToPlan(dayName: String, meal: Meal) {
        viewModelScope.launch {
            userId?.let { id ->
                val currentPlan = _weeklyMealPlan.value?.toMutableList() ?: mutableListOf()
                val dayExists = currentPlan.any { it.dayName.equals(dayName, ignoreCase = true) }
                val updatedPlan = if (dayExists) {
                    currentPlan.map {
                        if (it.dayName.equals(dayName, ignoreCase = true)) {
                            val newMeals = it.meals.toMutableList().apply { add(meal) }
                            it.copy(meals = newMeals)
                        } else {
                            it
                        }
                    }
                } else {
                    currentPlan.apply { add(DayMealPlan(dayName, mutableListOf(meal))) }
                }
                _weeklyMealPlan.value = updatedPlan
                // Sync with Firebase and Room
                FirebaseDatabase.getInstance().getReference("/users/$id/mealPlans/$dayName/${meal.idMeal}").setValue(meal)
                mealPlanDao.insertSingleDayMealPlan(meal.toMealPlanEntity(id, dayName))
            }
        }
    }

    // Delete a meal from the specified day and sync with Firebase and Room
    fun deleteMealFromPlan(dayName: String, meal: Meal) {
        viewModelScope.launch {
            userId?.let { id ->
                // Update the local meal plan
                val currentPlan = _weeklyMealPlan.value?.toMutableList() ?: mutableListOf()
                val updatedPlan = currentPlan.map {
                    if (it.dayName.equals(dayName, ignoreCase = true)) {
                        val newMeals = it.meals.toMutableList().apply { remove(meal) }
                        it.copy(meals = newMeals)
                    } else {
                        it
                    }
                }
                _weeklyMealPlan.value = updatedPlan

                // Remove from Firebase
                FirebaseDatabase.getInstance().getReference("/users/$id/mealPlans/$dayName/${meal.idMeal}").removeValue()

                // Remove from Room
                mealPlanDao.deleteMealPlanForDayAndMeal(id, dayName, meal.idMeal)

                // Refresh the UI immediately
                loadMealPlanFromDatabase()
            }
        }
    }

    // Clear the entire meal plan for the user from both Firebase and Room
    fun clearMealPlanForUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                mealPlanDao.clearMealPlan(userId)
                FirebaseDatabase.getInstance().getReference("/users/$userId/mealPlans").removeValue()
                _clearMealPlanResult.postValue(true)
                // Refresh the UI after clearing
                loadMealPlanFromDatabase()
            } catch (e: Exception) {
                _clearMealPlanResult.postValue(false)
            }
        }
    }

    // Clean up Firebase listeners when the ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        stopMealPlansSync()
    }
}

class MealPlanViewModelFactory(
    private val userRepository: UserRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MealPlanViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MealPlanViewModel(userRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}