package com.example.foodplanner.search.view

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import com.example.foodplanner.R
import com.example.foodplanner.core.model.local.repository.UserRepositoryImpl
import com.example.foodplanner.core.model.local.source.LocalDataSourceImpl
import com.example.foodplanner.core.model.local.source.UserDatabase
import com.example.foodplanner.core.model.remote.repository.MealRepositoryImpl
import com.example.foodplanner.core.model.remote.source.RemoteGsonDataImpl
import com.example.foodplanner.core.viewmodel.DataViewModel
import com.example.foodplanner.core.viewmodel.DataViewModelFactory
import com.example.foodplanner.meal_plan.viewModel.MealPlanViewModel
import com.example.foodplanner.meal_plan.viewModel.MealPlanViewModelFactory
import com.example.foodplanner.search.adapter.MealsAdapter
import com.example.foodplanner.search.viewmodel.SearchViewModel
import com.example.foodplanner.search.viewmodel.SearchViewModelFactory
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth

class SearchFragment : Fragment() {

    private val searchViewModel: SearchViewModel by viewModels {
        val mealRepository = MealRepositoryImpl(RemoteGsonDataImpl())
        SearchViewModelFactory(mealRepository)
    }

    private val dataViewModel: DataViewModel by activityViewModels {
        val userRepository = UserRepositoryImpl(
            LocalDataSourceImpl(UserDatabase.getDatabaseInstance(requireContext()).userDao()),
            FirebaseAuth.getInstance()
        )
        val mealRepository = MealRepositoryImpl(RemoteGsonDataImpl())
        val favouriteMealDao = UserDatabase.getDatabaseInstance(requireContext()).favouriteMealDao()
        DataViewModelFactory(userRepository, mealRepository, favouriteMealDao)
    }

    private val mealPlanViewModel: MealPlanViewModel by activityViewModels {
        val userRepository = UserRepositoryImpl(
            LocalDataSourceImpl(UserDatabase.getDatabaseInstance(requireContext()).userDao()),
            FirebaseAuth.getInstance()
        )
        MealPlanViewModelFactory(userRepository, requireContext())
    }

    private lateinit var etSearch: EditText
    private lateinit var cgSearchType: ChipGroup
    private lateinit var rvMeals: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var chipCountry: Chip
    private lateinit var chipIngredient: Chip
    private lateinit var chipCategory: Chip
    private lateinit var mealsAdapter: MealsAdapter
    private var navController: NavController? = null
    private var currentSearchType: SearchViewModel.SearchType? = null
    private var currentCountrySelection: String? = null
    private var currentIngredientSelection: String? = null
    private var currentCategorySelection: String? = null

    private val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_in_right)
        .setPopExitAnim(R.anim.slide_out_right)
        .build()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeUi(view)
        initObservers()
        setupSearchListener()
        updateChipText(null)
        navController = findNavController()
    }

    private fun initializeUi(view: View) {
        etSearch = view.findViewById(R.id.etSearch)
        cgSearchType = view.findViewById(R.id.cgSearchType)
        rvMeals = view.findViewById(R.id.rvMeals)
        progressBar = view.findViewById(R.id.progressBar)
        chipCountry = view.findViewById(R.id.chipCountry)
        chipIngredient = view.findViewById(R.id.chipIngredient)
        chipCategory = view.findViewById(R.id.chipCategory)

        chipCountry.isCloseIconVisible = false
        chipCountry.isChecked = false
        chipIngredient.isCloseIconVisible = false
        chipIngredient.isChecked = false
        chipCategory.isCloseIconVisible = false
        chipCategory.isChecked = false

        mealsAdapter = MealsAdapter(
            { mealId -> goToDetails(mealId) },
            this,
            mealPlanViewModel
        )
        rvMeals.layoutManager = LinearLayoutManager(requireContext())
        rvMeals.adapter = mealsAdapter

        etSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val query = v.text.toString().trim()
                if (query.isNotEmpty()) {
                    progressBar.visibility = View.VISIBLE
                    performSearch(query)
                    hideKeyboard()
                }
                true
            } else {
                false
            }
        }
    }

    private fun initObservers() {
        searchViewModel.searchResults.observe(viewLifecycleOwner) { meals ->
            Log.d("SearchFragment", "Meals updated: ${meals?.size ?: 0} items")
            mealsAdapter.submitList(meals)
            progressBar.visibility = View.GONE
            rvMeals.scrollToPosition(0)
        }
    }

    private fun setupSearchListener() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                Log.d("SearchFragment", "Before text changed: $s")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                Log.d("SearchFragment", "Text changed: $query, Current Search Type: $currentSearchType")
                if (query.isNotEmpty()) {
                    progressBar.visibility = View.VISIBLE
                    performSearch(query)
                } else {
                    if (currentSearchType != null && (currentCountrySelection != null || currentIngredientSelection != null || currentCategorySelection != null)) {
                        progressBar.visibility = View.VISIBLE
                        performSearch("")
                    } else {
                        mealsAdapter.submitList(emptyList())
                        progressBar.visibility = View.GONE
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
                Log.d("SearchFragment", "After text changed: ${s.toString()}")
            }
        })

        chipCountry.setOnClickListener {
            resetAllSelectionsExcept(SearchViewModel.SearchType.COUNTRY)
            currentSearchType = SearchViewModel.SearchType.COUNTRY
            Log.d("SearchFragment", "Opening BottomSheet for: COUNTRY")
            showSuggestionsBottomSheet()
        }

        chipIngredient.setOnClickListener {
            resetAllSelectionsExcept(SearchViewModel.SearchType.INGREDIENT)
            currentSearchType = SearchViewModel.SearchType.INGREDIENT
            Log.d("SearchFragment", "Opening BottomSheet for: INGREDIENT")
            showSuggestionsBottomSheet()
        }

        chipCategory.setOnClickListener {
            resetAllSelectionsExcept(SearchViewModel.SearchType.CATEGORY)
            currentSearchType = SearchViewModel.SearchType.CATEGORY
            Log.d("SearchFragment", "Opening BottomSheet for: CATEGORY")
            showSuggestionsBottomSheet()
        }

        chipCountry.setOnCloseIconClickListener {
            resetChip(chipCountry)
            performSearch("")
        }
        chipIngredient.setOnCloseIconClickListener {
            resetChip(chipIngredient)
            performSearch("")
        }
        chipCategory.setOnCloseIconClickListener {
            resetChip(chipCategory)
            performSearch("")
        }
    }

    private fun resetAllSelectionsExcept(exceptType: SearchViewModel.SearchType) {
        if (exceptType != SearchViewModel.SearchType.COUNTRY) {
            currentCountrySelection = null
            chipCountry.isChecked = false
            chipCountry.isCloseIconVisible = false
        }
        if (exceptType != SearchViewModel.SearchType.INGREDIENT) {
            currentIngredientSelection = null
            chipIngredient.isChecked = false
            chipIngredient.isCloseIconVisible = false
        }
        if (exceptType != SearchViewModel.SearchType.CATEGORY) {
            currentCategorySelection = null
            chipCategory.isChecked = false
            chipCategory.isCloseIconVisible = false
        }
        updateChipText(null)
        // إزالة استدعاء resetSelections لتجنب إفراغ الاختيارات في SearchViewModel
    }

    private fun resetChip(chip: Chip) {
        when (chip.id) {
            R.id.chipCountry -> {
                currentCountrySelection = null
                chip.isChecked = false
            }
            R.id.chipIngredient -> {
                currentIngredientSelection = null
                chip.isChecked = false
            }
            R.id.chipCategory -> {
                currentCategorySelection = null
                chip.isChecked = false
            }
        }
        currentSearchType = null
        updateChipText(null)
        chip.isCloseIconVisible = false
        searchViewModel.resetSelections()
    }

    private fun showSuggestionsBottomSheet() {
        val previousCountrySelection = currentCountrySelection
        val previousIngredientSelection = currentIngredientSelection
        val previousCategorySelection = currentCategorySelection

        val bottomSheet = SuggestionsBottomSheet.newInstance(currentSearchType ?: SearchViewModel.SearchType.COUNTRY) { selectedSuggestion ->
            if (selectedSuggestion != null) {
                when (currentSearchType) {
                    SearchViewModel.SearchType.COUNTRY -> {
                        currentCountrySelection = selectedSuggestion
                        chipCountry.isChecked = true
                        chipCountry.isCloseIconVisible = true
                        searchViewModel.setSelectedCategory(SearchViewModel.SearchType.COUNTRY, selectedSuggestion)
                        updateChipText(selectedSuggestion)
                        searchViewModel.searchMealsByName("", currentSearchType)
                    }
                    SearchViewModel.SearchType.INGREDIENT -> {
                        currentIngredientSelection = selectedSuggestion
                        chipIngredient.isChecked = true
                        chipIngredient.isCloseIconVisible = true
                        searchViewModel.setSelectedCategory(SearchViewModel.SearchType.INGREDIENT, selectedSuggestion)
                        updateChipText(selectedSuggestion)
                        searchViewModel.searchMealsByName("", currentSearchType)
                    }
                    SearchViewModel.SearchType.CATEGORY -> {
                        currentCategorySelection = selectedSuggestion
                        chipCategory.isChecked = true
                        chipCategory.isCloseIconVisible = true
                        searchViewModel.setSelectedCategory(SearchViewModel.SearchType.CATEGORY, selectedSuggestion)
                        updateChipText(selectedSuggestion)
                        searchViewModel.searchMealsByName("", currentSearchType)
                    }
                    else -> {
                        progressBar.visibility = View.GONE
                    }
                }
            } else {
                // الاحتفاظ بالاختيار السابق إذا كان موجودًا
                when (currentSearchType) {
                    SearchViewModel.SearchType.COUNTRY -> {
                        if (previousCountrySelection != null) {
                            currentCountrySelection = previousCountrySelection
                            chipCountry.isChecked = true
                            chipCountry.isCloseIconVisible = true
                            chipCountry.text = previousCountrySelection
                            searchViewModel.setSelectedCategory(SearchViewModel.SearchType.COUNTRY, previousCountrySelection)
                            searchViewModel.searchMealsByName("", currentSearchType)
                        } else {
                            currentCountrySelection = null
                            chipCountry.isChecked = false
                            chipCountry.isCloseIconVisible = false
                            chipCountry.text = "Country"
                            searchViewModel.resetSelections()
                            mealsAdapter.submitList(emptyList())
                        }
                    }
                    SearchViewModel.SearchType.INGREDIENT -> {
                        if (previousIngredientSelection != null) {
                            currentIngredientSelection = previousIngredientSelection
                            chipIngredient.isChecked = true
                            chipIngredient.isCloseIconVisible = true
                            chipIngredient.text = previousIngredientSelection
                            searchViewModel.setSelectedCategory(SearchViewModel.SearchType.INGREDIENT, previousIngredientSelection)
                            searchViewModel.searchMealsByName("", currentSearchType)
                        } else {
                            currentIngredientSelection = null
                            chipIngredient.isChecked = false
                            chipIngredient.isCloseIconVisible = false
                            chipIngredient.text = "Ingredient"
                            searchViewModel.resetSelections()
                            mealsAdapter.submitList(emptyList())
                        }
                    }
                    SearchViewModel.SearchType.CATEGORY -> {
                        if (previousCategorySelection != null) {
                            currentCategorySelection = previousCategorySelection
                            chipCategory.isChecked = true
                            chipCategory.isCloseIconVisible = true
                            chipCategory.text = previousCategorySelection
                            searchViewModel.setSelectedCategory(SearchViewModel.SearchType.CATEGORY, previousCategorySelection)
                            searchViewModel.searchMealsByName("", currentSearchType)
                        } else {
                            currentCategorySelection = null
                            chipCategory.isChecked = false
                            chipCategory.isCloseIconVisible = false
                            chipCategory.text = "Category"
                            searchViewModel.resetSelections()
                            mealsAdapter.submitList(emptyList())
                        }
                    }
                    else -> {}
                }
                progressBar.visibility = View.GONE
            }
        }
        bottomSheet.show(childFragmentManager, SuggestionsBottomSheet.TAG)
    }

    private fun performSearch(query: String) {
        Log.d("SearchFragment", "Performing search for query: $query, Type: $currentSearchType")
        searchViewModel.searchMealsByName(query, currentSearchType)
    }

    private fun updateChipText(selectedSuggestion: String?) {
        val countryText = currentCountrySelection ?: "Country"
        val ingredientText = currentIngredientSelection ?: "Ingredient"
        val categoryText = currentCategorySelection ?: "Category"

        chipCountry.text = countryText
        chipCountry.isCloseIconVisible = currentCountrySelection != null
        chipCountry.isChecked = currentCountrySelection != null

        chipIngredient.text = ingredientText
        chipIngredient.isCloseIconVisible = currentIngredientSelection != null
        chipIngredient.isChecked = currentIngredientSelection != null

        chipCategory.text = categoryText
        chipCategory.isCloseIconVisible = currentCategorySelection != null
        chipCategory.isChecked = currentCategorySelection != null
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        imm?.hideSoftInputFromWindow(etSearch.windowToken, 0)
    }

    private fun goToDetails(id: String) {
        dataViewModel.setItemDetails(id)
        navController?.navigate(R.id.action_details, null, navOptions)
    }
}