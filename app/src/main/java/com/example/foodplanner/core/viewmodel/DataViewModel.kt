package com.example.foodplanner.core.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodplanner.core.model.remote.source.FavouriteMealDao
import com.example.foodplanner.core.model.local.repository.UserRepository
import com.example.foodplanner.core.model.local.repository.UserRepositoryImpl
import com.example.foodplanner.core.model.local.source.LocalDataSourceImpl
import com.example.foodplanner.core.model.local.source.UserDatabase
import com.example.foodplanner.core.model.remote.Meal
import com.example.foodplanner.core.model.remote.repository.MealRepository
import com.example.foodplanner.core.model.remote.repository.MealRepositoryImpl
import com.example.foodplanner.core.model.remote.source.RemoteGsonDataImpl
import com.example.foodplanner.core.model.entity.toFavouriteMealEntity
import com.example.foodplanner.core.model.entity.toMeal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch

class DataViewModel(
    private val userRepository: UserRepository,
    private val mealRepository: MealRepository,
    private val favouriteMealDao: FavouriteMealDao
) : ViewModel() {

    companion object {
        // Factory method to create an instance of DataViewModel with required dependencies
        fun create(context: Context): DataViewModel {
            val database = UserDatabase.getDatabaseInstance(context)
            val userRepository = UserRepositoryImpl(
                LocalDataSourceImpl(database.userDao()),
                FirebaseAuth.getInstance()
            )
            val mealRepository = MealRepositoryImpl(RemoteGsonDataImpl())
            return DataViewModel(userRepository, mealRepository, database.favouriteMealDao())
        }
    }

    private val _favouritesList = MutableLiveData<MutableSet<String>>()
    private val _meals = MutableLiveData<MutableList<Meal>>()
    val meals: LiveData<MutableList<Meal>> get() = _meals

    private val _isFavourite = MutableLiveData<Boolean>()
    val isFavourite: LiveData<Boolean> get() = _isFavourite

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _clearFavouriteResult = MutableLiveData<Boolean>()
    val clearFavouriteResult: LiveData<Boolean> get() = _clearFavouriteResult

    private val _categorySearch = MutableLiveData<String?>()
    val categorySearch: LiveData<String?> get() = _categorySearch

    private val _itemDetails = MutableLiveData<String>()
    val itemDetails: LiveData<String> get() = _itemDetails

    private val _cuisinesData = MutableLiveData<List<String>>()
    val cuisinesData: LiveData<List<String>> get() = _cuisinesData

    private var currentUserId: String? = null
    private var favoritesListener: ChildEventListener? = null

    init {
        _categorySearch.value = null
        _isLoading.value = false
        loadCurrentUser()
    }

    // Load the current user's ID and initialize favorite items if a user is logged in
    private fun loadCurrentUser() {
        viewModelScope.launch {
            currentUserId = userRepository.getCurrentUserId()
            if (currentUserId == null) {
                Log.e("DataViewModel", "No logged-in user found, favourites will not be loaded")
                _favouritesList.value = mutableSetOf()
                _meals.value = mutableListOf()
            } else {
                Log.d("DataViewModel", "Loaded current user ID: $currentUserId")
                loadFavoriteItems()
            }
        }
    }

    // Start syncing favorite meals with Firebase Realtime Database for the given user
    fun startFavoritesSync(userId: String) {
        if (favoritesListener != null) return
        _isLoading.value = true
        currentUserId = userId
        val favoritesRef = FirebaseDatabase.getInstance().getReference("/users/$userId/favorites")
        favoritesListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val meal = snapshot.getValue(Meal::class.java)
                meal?.let {
                    viewModelScope.launch {
                        favouriteMealDao.insertFavouriteMeal(it.toFavouriteMealEntity(userId))
                        loadFavoriteItems()
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val mealId = snapshot.key
                mealId?.let {
                    viewModelScope.launch {
                        favouriteMealDao.deleteFavouriteMeal(it, userId)
                        loadFavoriteItems()
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("DataViewModel", "Failed to sync favorites", error.toException())
                _isLoading.value = false
            }
        }
        favoritesRef.addChildEventListener(favoritesListener!!)
        viewModelScope.launch {
            loadFavoriteItems()
            _isLoading.value = false
        }
    }

    // Stop syncing favorite meals by removing the Firebase listener
    fun stopFavoritesSync() {
        currentUserId?.let { uid ->
            val favoritesRef = FirebaseDatabase.getInstance().getReference("/users/$uid/favorites")
            favoritesListener?.let { favoritesRef.removeEventListener(it) }
        }
        favoritesListener = null
    }

    // Load favorite meal IDs from the local database and update the meals list
    private fun loadFavoriteItems() {
        viewModelScope.launch {
            currentUserId?.let { userId ->
                val favouriteMeals = favouriteMealDao.getAllFavouriteMeals(userId)
                val mealIds = favouriteMeals.map { it.idMeal }.toMutableSet()
                if (_favouritesList.value != mealIds) {
                    _favouritesList.value = mealIds
                    getMeals()
                }
            }
        }
    }

    // Fetch favorite meals from the local database and update the LiveData
    private fun getMeals() {
        viewModelScope.launch {
            currentUserId?.let { userId ->
                val favouriteMeals = favouriteMealDao.getAllFavouriteMeals(userId)
                Log.d("DataViewModel", "Retrieved favourite meals for user $userId: $favouriteMeals")
                val meals = favouriteMeals.map { it.toMeal() }.toMutableList()
                _meals.value = meals
            }
        }
    }

    // Add or remove a meal from favorites and sync with Firebase and local database
    suspend fun changeFavouriteState(recipeId: String, isChange: Boolean) {
        currentUserId?.let { userId ->
            val currentFavourites = _favouritesList.value?.toMutableSet() ?: mutableSetOf()
            var currentFavouriteState = currentFavourites.contains(recipeId)
            if (isChange) {
                if (currentFavouriteState) {
                    FirebaseDatabase.getInstance().getReference("/users/$userId/favorites/$recipeId").removeValue()
                    favouriteMealDao.deleteFavouriteMeal(recipeId, userId)
                    Log.d("DataViewModel", "Deleted meal $recipeId from favourites for user $userId")
                    currentFavourites.remove(recipeId)
                    currentFavouriteState = false
                } else {
                    val meal = mealRepository.getMealById(recipeId)
                    FirebaseDatabase.getInstance().getReference("/users/$userId/favorites/$recipeId").setValue(meal)
                    val favouriteMeal = meal.toFavouriteMealEntity(userId)
                    favouriteMealDao.insertFavouriteMeal(favouriteMeal)
                    Log.d("DataViewModel", "Added meal $recipeId to favourites for user $userId")
                    currentFavourites.add(recipeId)
                    currentFavouriteState = true
                }
                _favouritesList.value = currentFavourites
                getMeals()
            }
            _isFavourite.value = currentFavouriteState
        }
    }

    fun clearFavouriteForUser(userId: String) {
        viewModelScope.launch {
            try {
                // Clear from Room
                favouriteMealDao.clearFavouriteMeals(userId)
                // Clear from Firebase
                FirebaseDatabase.getInstance().getReference("/users/$userId/favorites").removeValue()
                _clearFavouriteResult.postValue(true)
                // Refresh the UI after clearing
                loadFavoriteItems()
            } catch (e: Exception) {
                Log.e("DataViewModel", "Failed to clear favorites for user $userId", e)
                _clearFavouriteResult.postValue(false)
            }
        }
    }

    // Retrieve a meal by its ID from the remote repository
    suspend fun getMealById(mealId: String): Meal? {
        return mealRepository.getMealById(mealId)
    }

    // Update the category used for searching or filtering meals
    fun updateSearchCategory(category: String?) {
        _categorySearch.value = category
        Log.d("DataViewModel", "Updated search category: $category")
    }

    fun setItemDetails(id: String) {
        _itemDetails.value = id
    }


    // Clean up resources by stopping Firebase sync when the ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        stopFavoritesSync()
    }
}