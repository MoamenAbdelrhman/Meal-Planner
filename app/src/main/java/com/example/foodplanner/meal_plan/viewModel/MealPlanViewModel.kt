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
                startMealPlansSync(userId!!) // بدء المزامنة عند التهيئة
            }
        }
    }

    // بدء المزامنة مع Firebase لكل يوم
    fun startMealPlansSync(userId: String) {
        this.userId = userId
        DAYS_OF_WEEK.forEach { day ->
            val dayRef = FirebaseDatabase.getInstance().getReference("/users/$userId/mealPlans/$day")
            val listener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val meal = snapshot.getValue(Meal::class.java)
                    meal?.let {
                        viewModelScope.launch {
                            mealPlanDao.insertSingleDayMealPlan(it.toMealPlanEntity(userId, day))
                            loadMealPlanFromDatabase()
                        }
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    val mealId = snapshot.key
                    mealId?.let {
                        viewModelScope.launch {
                            mealPlanDao.deleteMealPlanForDayAndMeal(userId, day, it)
                            loadMealPlanFromDatabase()
                        }
                    }
                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    val meal = snapshot.getValue(Meal::class.java)
                    meal?.let {
                        it.populateIngredientsWithMeasures() // ملء القائمة بعد التحويل
                        viewModelScope.launch {
                            mealPlanDao.insertSingleDayMealPlan(it.toMealPlanEntity(userId, day))
                            loadMealPlanFromDatabase()
                        }
                    }
                }
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onCancelled(error: DatabaseError) {
                    Log.e("MealPlanViewModel", "فشل في مزامنة خطة $day", error.toException())
                }
            }
            dayRef.addChildEventListener(listener)
            mealPlanListeners[day] = listener
        }
    }

    // إيقاف المزامنة
    fun stopMealPlansSync() {
        userId?.let { uid ->
            DAYS_OF_WEEK.forEach { day ->
                val dayRef = FirebaseDatabase.getInstance().getReference("/users/$uid/mealPlans/$day")
                mealPlanListeners[day]?.let { dayRef.removeEventListener(it) }
            }
            mealPlanListeners.clear()
        }
    }

    private fun loadMealPlanFromDatabase() {
        viewModelScope.launch {
            userId?.let { id ->
                val savedPlanEntities = mealPlanDao.getMealPlan(id)
                val savedPlan = savedPlanEntities.toDayMealPlans()
                val fullWeekPlan = DAYS_OF_WEEK.map { dayName ->
                    savedPlan.find { it.dayName.equals(dayName, ignoreCase = true) } ?: DayMealPlan(dayName)
                }
                _weeklyMealPlan.value = fullWeekPlan
                _isLoading.value = false
            }
        }
    }

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
                // تحديث Firebase و Room
                FirebaseDatabase.getInstance().getReference("/users/$id/mealPlans/$dayName/${meal.idMeal}").setValue(meal)
                mealPlanDao.insertSingleDayMealPlan(meal.toMealPlanEntity(id, dayName))
            }
        }
    }

    fun deleteMealFromPlan(dayName: String, meal: Meal) {
        viewModelScope.launch {
            userId?.let { id ->
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
                // تحديث Firebase و Room
                FirebaseDatabase.getInstance().getReference("/users/$id/mealPlans/$dayName/${meal.idMeal}").removeValue()
                mealPlanDao.clearMealPlan(id)
                updatedPlan.forEach { day ->
                    day.meals.forEach { m ->
                        mealPlanDao.insertSingleDayMealPlan(m.toMealPlanEntity(id, day.dayName))
                    }
                }
            }
        }
    }

    fun clearMealPlanForUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                mealPlanDao.clearMealPlan(userId)
                FirebaseDatabase.getInstance().getReference("/users/$userId/mealPlans").removeValue()
                _clearMealPlanResult.postValue(true)
            } catch (e: Exception) {
                _clearMealPlanResult.postValue(false)
            }
        }
    }

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