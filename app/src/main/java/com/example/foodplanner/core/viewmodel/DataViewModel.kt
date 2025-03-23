package com.example.foodplanner.core.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodplanner.core.model.FavouriteMealDao
import com.example.foodplanner.core.model.FavouriteMealEntity
import com.example.foodplanner.core.model.local.repository.UserRepository
import com.example.foodplanner.core.model.local.repository.UserRepositoryImpl
import com.example.foodplanner.core.model.local.source.LocalDataSourceImpl
import com.example.foodplanner.core.model.local.source.UserDatabase
import com.example.foodplanner.core.model.remote.Meal
import com.example.foodplanner.core.model.remote.repository.MealRepository
import com.example.foodplanner.core.model.remote.repository.MealRepositoryImpl
import com.example.foodplanner.core.model.remote.source.RemoteGsonDataImpl
import com.example.foodplanner.core.model.toFavouriteMealEntity
import com.example.foodplanner.core.model.toMeal
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

    private val _categorySearch = MutableLiveData<String?>()
    val categorySearch: LiveData<String?> get() = _categorySearch

    private val _mainCuisine = MutableLiveData<String?>()
    val mainCuisine: LiveData<String?> get() = _mainCuisine

    private val _itemDetails = MutableLiveData<String>()
    val itemDetails: LiveData<String> get() = _itemDetails

    private val _cuisinesData = MutableLiveData<List<String>>()
    val cuisinesData: LiveData<List<String>> get() = _cuisinesData

    private var currentUserId: String? = null
    private var favoritesListener: ChildEventListener? = null

    init {
        _categorySearch.value = null
        _mainCuisine.value = null
        _isLoading.value = false
        loadCurrentUser()
    }

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

    fun stopFavoritesSync() {
        currentUserId?.let { uid ->
            val favoritesRef = FirebaseDatabase.getInstance().getReference("/users/$uid/favorites")
            favoritesListener?.let { favoritesRef.removeEventListener(it) }
        }
        favoritesListener = null
    }

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

    suspend fun getMealById(mealId: String): Meal? {
        return mealRepository.getMealById(mealId)
    }

    fun updateSearchCategory(category: String?) {
        _categorySearch.value = category
        Log.d("DataViewModel", "Updated search category: $category")
    }

    fun setItemDetails(id: String) {
        _itemDetails.value = id
    }

    fun setCuisines(cuisines: List<String>) {
        _cuisinesData.value = cuisines
        viewModelScope.launch {
            userRepository.updateCuisines(cuisines)
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopFavoritesSync()
    }
}