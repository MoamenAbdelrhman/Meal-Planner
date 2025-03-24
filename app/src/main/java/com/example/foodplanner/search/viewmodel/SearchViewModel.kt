package com.example.foodplanner.search.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.foodplanner.core.model.remote.Meal
import com.example.foodplanner.core.model.remote.repository.MealRepository
import com.example.foodplanner.search.model.SuggestionItem
import com.example.foodplanner.utils.NetworkUtils
import kotlinx.coroutines.launch

class SearchViewModel(private val mealRepository: MealRepository) : ViewModel() {

    private val _searchResults = MutableLiveData<List<Meal>>()
    val searchResults: LiveData<List<Meal>> get() = _searchResults

    private val _suggestions = MutableLiveData<List<SuggestionItem>>()
    val suggestions: LiveData<List<SuggestionItem>> get() = _suggestions

    private var countries: List<SuggestionItem> = emptyList()
    private var ingredients: List<SuggestionItem> = emptyList()
    private var categories: List<SuggestionItem> = emptyList()
    private var selectedCountry: String? = null
    private var selectedIngredient: String? = null
    private var selectedCategory: String? = null

    init {
        loadSuggestions()
    }

    // Loads initial suggestions (countries, categories, ingredients) from the repository
    fun loadSuggestions() {
        viewModelScope.launch {
            try {
                val fetchedCountries = mealRepository.getCuisines().meals.map { it.strArea }
                val countryCodeMap = mapOf(
                    "American" to "us", "British" to "gb", "Canadian" to "ca", "Chinese" to "cn",
                    "French" to "fr", "Indian" to "in", "Italian" to "it", "Japanese" to "jp",
                    "Mexican" to "mx", "Spanish" to "es", "Croatian" to "hr", "Dutch" to "nl",
                    "Egyptian" to "eg", "Filipino" to "ph", "Greek" to "gr", "Moroccan" to "ma",
                    "Polish" to "pl", "Portuguese" to "pt", "Russian" to "ru", "Thai" to "th",
                    "Tunisian" to "tn", "Turkish" to "tr", "Ukrainian" to "ua", "Uruguayan" to "uy",
                    "Vietnamese" to "vn", "Irish" to "ie", "Jamaican" to "jm", "Kenyan" to "ke",
                    "Malaysian" to "my"
                )
                countries = fetchedCountries.map { country ->
                    val code = countryCodeMap[country]
                    val flagUrl = if (code != null) "https://flagcdn.com/w80/${code.lowercase()}.png" else null
                    SuggestionItem(name = country, imageUrl = flagUrl, type = SearchType.COUNTRY)
                }
                Log.d("SearchViewModel", "Countries loaded: ${countries.size}, Names: ${countries.map { it.name }}")

                categories = mealRepository.getCategories().categories.map { category ->
                    SuggestionItem(
                        name = category.strCategory,
                        imageUrl = category.strCategoryThumb,
                        type = SearchType.CATEGORY
                    )
                }
                Log.d("SearchViewModel", "Categories loaded: ${categories.size}")

                ingredients = mealRepository.getIngredients().meals.map { ingredient ->
                    val imageUrl = "https://www.themealdb.com/images/ingredients/${ingredient.strIngredient.lowercase().replace(" ", "_")}.png"
                    SuggestionItem(name = ingredient.strIngredient, imageUrl = imageUrl, type = SearchType.INGREDIENT)
                }
                Log.d("SearchViewModel", "Ingredients loaded: ${ingredients.size}")

                _suggestions.value = countries // Default to countries
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error loading suggestions: ${e.message}")
            }
        }
    }

    // Updates the suggestions list based on the selected search type (country, ingredient, category)
    fun updateSuggestions(searchType: SearchType, context: android.content.Context) {
        Log.d("SearchViewModel", "Updating suggestions for: $searchType")
        if (!NetworkUtils.isInternetAvailable(context)) {
            Log.d("SearchViewModel", "No internet, cannot update suggestions")
            _suggestions.value = emptyList()
            return
        }

        if (countries.isEmpty() || ingredients.isEmpty() || categories.isEmpty()) {
            Log.d("SearchViewModel", "Suggestions are empty, reloading...")
            loadSuggestions()
        }

        _suggestions.value = when (searchType) {
            SearchType.COUNTRY -> countries
            SearchType.INGREDIENT -> ingredients
            SearchType.CATEGORY -> categories
        }
        Log.d("SearchViewModel", "New suggestions size: ${_suggestions.value?.size}")
    }

    // Filters suggestions based on the user's query input
    fun filterSuggestions(query: String, searchType: SearchType) {
        val allSuggestions = when (searchType) {
            SearchType.COUNTRY -> countries
            SearchType.INGREDIENT -> ingredients
            SearchType.CATEGORY -> categories
        }
        Log.d("SearchViewModel", "Filtering suggestions for query: $query, Type: $searchType")
        if (query.isEmpty()) {
            _suggestions.value = allSuggestions
        } else {
            _suggestions.value = allSuggestions.filter {
                it.name.lowercase().contains(query.lowercase())
            }
        }
    }

    // Searches for meals based on the query and the current search type
    fun searchMealsByName(query: String, searchType: SearchType? = null) {
        viewModelScope.launch {
            try {
                val results = when {
                    searchType == null || (selectedCountry == null && selectedIngredient == null && selectedCategory == null) -> {
                        if (query.isEmpty()) {
                            Log.d("SearchViewModel", "Empty query for general search, returning empty list")
                            emptyList()
                        } else {
                            val searchResults = mealRepository.getMealsBySearch(query).meals?.filter {
                                it.strMeal?.contains(query, ignoreCase = true) == true
                            } ?: emptyList()
                            if (searchResults.isEmpty()) {
                                Log.w("SearchViewModel", "No results found for general search with query: $query")
                            }
                            searchResults
                        }
                    }
                    searchType == SearchType.COUNTRY && selectedCountry != null -> {
                        val baseMeals = mealRepository.getMealsByArea(selectedCountry!!) ?: emptyList()
                        if (query.isEmpty()) baseMeals else baseMeals.filter { meal ->
                            meal.strMeal?.contains(query, ignoreCase = true) == true
                        }
                    }
                    searchType == SearchType.INGREDIENT && selectedIngredient != null -> {
                        val baseMeals = mealRepository.getMealsByIngredient(selectedIngredient!!) ?: emptyList()
                        if (query.isEmpty()) baseMeals else baseMeals.filter { meal ->
                            meal.strMeal?.contains(query, ignoreCase = true) == true
                        }
                    }
                    searchType == SearchType.CATEGORY && selectedCategory != null -> {
                        val baseMeals = mealRepository.getMealsByCategory(selectedCategory!!) ?: emptyList()
                        if (query.isEmpty()) baseMeals else baseMeals.filter { meal ->
                            meal.strMeal?.contains(query, ignoreCase = true) == true
                        }
                    }
                    else -> {
                        Log.w("SearchViewModel", "No category selected for search type: $searchType")
                        emptyList()
                    }
                }
                _searchResults.value = results
                Log.d("SearchViewModel", "Search meals by name for query: $query, Type: $searchType, Size: ${results.size}, Selected: Country=$selectedCountry, Ingredient=$selectedIngredient, Category=$selectedCategory")
            } catch (e: Exception) {
                Log.e("SearchViewModel", "Error searching meals by name: ${e.message}")
                _searchResults.value = emptyList()
            }
        }
    }

    // Sets the selected filter (country, ingredient, or category) and clears others
    fun setSelectedCategory(type: SearchType, value: String?) {
        when (type) {
            SearchType.COUNTRY -> {
                selectedCountry = value
                selectedIngredient = null
                selectedCategory = null
            }
            SearchType.INGREDIENT -> {
                selectedIngredient = value
                selectedCountry = null
                selectedCategory = null
            }
            SearchType.CATEGORY -> {
                selectedCategory = value
                selectedCountry = null
                selectedIngredient = null
            }
        }
        Log.d("SearchViewModel", "Selected category updated: Country=$selectedCountry, Ingredient=$selectedIngredient, Category=$selectedCategory")
    }

    // Resets all selections and clears search results
    fun resetSelections() {
        selectedCountry = null
        selectedIngredient = null
        selectedCategory = null
        _searchResults.value = emptyList()
        Log.d("SearchViewModel", "All selections reset")
    }

    fun getSelectedCountry(): String? = selectedCountry
    fun getSelectedIngredient(): String? = selectedIngredient
    fun getSelectedCategory(): String? = selectedCategory

    // Returns the current active search type based on selections
    fun getCurrentSearchType(): SearchType? {
        return when {
            selectedCountry != null -> SearchType.COUNTRY
            selectedIngredient != null -> SearchType.INGREDIENT
            selectedCategory != null -> SearchType.CATEGORY
            else -> null
        }
    }

    // Checks if there are any active selections
    fun hasSelections(): Boolean {
        return selectedCountry != null || selectedIngredient != null || selectedCategory != null
    }

    enum class SearchType {
        COUNTRY, INGREDIENT, CATEGORY
    }
}

class SearchViewModelFactory(private val mealRepository: MealRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SearchViewModel(mealRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}