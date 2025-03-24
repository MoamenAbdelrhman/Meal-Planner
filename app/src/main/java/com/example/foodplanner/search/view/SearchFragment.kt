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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.example.foodplanner.R
import com.example.foodplanner.core.model.local.repository.UserRepositoryImpl
import com.example.foodplanner.core.model.local.source.LocalDataSourceImpl
import com.example.foodplanner.core.model.local.source.UserDatabase
import com.example.foodplanner.core.model.remote.Meal
import com.example.foodplanner.core.model.remote.repository.MealRepositoryImpl
import com.example.foodplanner.core.model.remote.source.RemoteGsonDataImpl
import com.example.foodplanner.core.util.CreateMaterialAlertDialogBuilder
import com.example.foodplanner.core.viewmodel.DataViewModel
import com.example.foodplanner.core.viewmodel.DataViewModelFactory
import com.example.foodplanner.meal_plan.viewModel.MealPlanViewModel
import com.example.foodplanner.meal_plan.viewModel.MealPlanViewModelFactory
import com.example.foodplanner.search.adapter.MealsAdapter
import com.example.foodplanner.search.viewmodel.SearchViewModel
import com.example.foodplanner.search.viewmodel.SearchViewModelFactory
import com.example.foodplanner.utils.NetworkUtils
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import android.app.AlertDialog

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
    private lateinit var noResultsTextView: TextView
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
    private var isGuest: Boolean = false

    companion object {
        private const val KEY_SEARCH_TEXT = "search_text"
        private const val KEY_SEARCH_TYPE = "search_type"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isGuest = FirebaseAuth.getInstance().currentUser == null ||
                requireActivity().intent.getBooleanExtra("IS_GUEST", false)

        savedInstanceState?.let {
            currentSearchType = it.getSerializable(KEY_SEARCH_TYPE) as? SearchViewModel.SearchType
        }

        currentSearchType = searchViewModel.getCurrentSearchType()
        syncSelectionsWithViewModel()
    }

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
        navController = findNavController()

        savedInstanceState?.let {
            etSearch.setText(it.getString(KEY_SEARCH_TEXT))
        }
        updateChipText(null)

        val query = etSearch.text.toString().trim()
        if (searchViewModel.hasSelections() || query.isNotEmpty()) {
            performSearch(query)
        }
    }

    // Initializes UI components and configures the RecyclerView with its adapter
    private fun initializeUi(view: View) {
        etSearch = view.findViewById(R.id.etSearch)
        cgSearchType = view.findViewById(R.id.cgSearchType)
        rvMeals = view.findViewById(R.id.rvMeals)
        progressBar = view.findViewById(R.id.progressBar)
        chipCountry = view.findViewById(R.id.chipCountry)
        chipIngredient = view.findViewById(R.id.chipIngredient)
        chipCategory = view.findViewById(R.id.chipCategory)
        noResultsTextView = view.findViewById(R.id.noResultsTextView)

        chipCountry.isCloseIconVisible = searchViewModel.getSelectedCountry() != null
        chipCountry.isChecked = searchViewModel.getSelectedCountry() != null
        chipIngredient.isCloseIconVisible = searchViewModel.getSelectedIngredient() != null
        chipIngredient.isChecked = searchViewModel.getSelectedIngredient() != null
        chipCategory.isCloseIconVisible = searchViewModel.getSelectedCategory() != null
        chipCategory.isChecked = searchViewModel.getSelectedCategory() != null

        chipCountry.isCloseIconVisible = false
        chipCountry.isChecked = false
        chipIngredient.isCloseIconVisible = false
        chipIngredient.isChecked = false
        chipCategory.isCloseIconVisible = false
        chipCategory.isChecked = false

        mealsAdapter = MealsAdapter(
            onItemClick = { mealId -> goToDetails(mealId) },
            onAddToPlanClick = { meal -> handleAddToPlanClick(meal) },
            fragment = this
        )
        rvMeals.layoutManager = LinearLayoutManager(requireContext())
        rvMeals.adapter = mealsAdapter

        etSearch.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                val query = v.text.toString().trim()
                if (query.isNotEmpty()) {
                    performSearchWithInternetCheck(query)
                    hideKeyboard()
                }
                true
            } else {
                false
            }
        }
    }

    // Sets up observers to update the UI when search results change
    private fun initObservers() {
        searchViewModel.searchResults.observe(viewLifecycleOwner) { meals ->
            mealsAdapter.submitList(meals ?: emptyList())
            progressBar.visibility = View.GONE
            rvMeals.scrollToPosition(0)
            val query = etSearch.text.toString().trim()
            val hasActiveFilter = currentCountrySelection != null || currentIngredientSelection != null || currentCategorySelection != null
            if (meals.isNullOrEmpty() && (query.isNotEmpty() || hasActiveFilter)) {
                noResultsTextView.visibility = View.VISIBLE
                val categoryMessage = when {
                    currentCountrySelection != null -> " in $currentCountrySelection cuisine"
                    currentIngredientSelection != null -> " with $currentIngredientSelection"
                    currentCategorySelection != null -> " in $currentCategorySelection category"
                    else -> ""
                }
                noResultsTextView.text = if (query.isNotEmpty()) {
                    "No meals found for '$query'$categoryMessage."
                } else {
                    "No meals found$categoryMessage."
                }
            } else {
                noResultsTextView.visibility = View.GONE
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SEARCH_TEXT, etSearch.text.toString())
        outState.putSerializable(KEY_SEARCH_TYPE, currentSearchType)
        Log.d(
            "SearchFragment",
            "onSaveInstanceState called, Search text: ${etSearch.text}, Type: $currentSearchType"
        )
    }

    // Syncs fragment's search type with the ViewModel's current selections
    private fun syncSelectionsWithViewModel() {
        val country = searchViewModel.getSelectedCountry()
        val ingredient = searchViewModel.getSelectedIngredient()
        val category = searchViewModel.getSelectedCategory()

        if (country != null) {
            currentSearchType = SearchViewModel.SearchType.COUNTRY
        } else if (ingredient != null) {
            currentSearchType = SearchViewModel.SearchType.INGREDIENT
        } else if (category != null) {
            currentSearchType = SearchViewModel.SearchType.CATEGORY
        }
        Log.d("SearchFragment", "Synced with ViewModel: Country=$country, Ingredient=$ingredient, Category=$category")
    }

    // Configures listeners for search input and filter chips
    private fun setupSearchListener() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    progressBar.visibility = View.VISIBLE
                    performSearch(query)
                } else if (searchViewModel.hasSelections()) {
                    progressBar.visibility = View.VISIBLE
                    performSearch("")
                } else {
                    mealsAdapter.submitList(emptyList())
                    progressBar.visibility = View.GONE
                    noResultsTextView.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        chipCountry.setOnClickListener {
            currentSearchType = SearchViewModel.SearchType.COUNTRY
            showSuggestionsBottomSheet()
        }

        chipIngredient.setOnClickListener {
            currentSearchType = SearchViewModel.SearchType.INGREDIENT
            showSuggestionsBottomSheet()
        }

        chipCategory.setOnClickListener {
            currentSearchType = SearchViewModel.SearchType.CATEGORY
            showSuggestionsBottomSheet()
        }

        chipCountry.setOnCloseIconClickListener {
            resetChip(chipCountry)
        }
        chipIngredient.setOnCloseIconClickListener {
            resetChip(chipIngredient)
        }
        chipCategory.setOnCloseIconClickListener {
            resetChip(chipCategory)
        }
    }

    // Performs a search after checking internet availability
    private fun performSearchWithInternetCheck(query: String) {
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            progressBar.visibility = View.VISIBLE
            performSearch(query)
        } else {
            progressBar.visibility = View.GONE
            showNoInternetDialog()
        }
    }

    // Displays a dialog when there’s no internet connection
    private fun showNoInternetDialog() {
        CreateMaterialAlertDialogBuilder.createMaterialAlertDialogBuilderOk(
            context = requireContext(),
            title = "No Internet Connection",
            message = "Please connect to the internet to search for meals.",
            positiveBtnMsg = "OK",
            positiveBtnFun = {}
        )
    }

    // Triggers a search with the given query and current search type
    private fun performSearch(query: String) {
        searchViewModel.searchMealsByName(query, currentSearchType)
    }

    private fun resetAllSelectionsExcept(exceptType: SearchViewModel.SearchType) {
        if (exceptType != SearchViewModel.SearchType.COUNTRY) {
            searchViewModel.setSelectedCategory(SearchViewModel.SearchType.COUNTRY, null)
            chipCountry.isChecked = false
            chipCountry.isCloseIconVisible = false
        }
        if (exceptType != SearchViewModel.SearchType.INGREDIENT) {
            searchViewModel.setSelectedCategory(SearchViewModel.SearchType.INGREDIENT, null)
            chipIngredient.isChecked = false
            chipIngredient.isCloseIconVisible = false
        }
        if (exceptType != SearchViewModel.SearchType.CATEGORY) {
            searchViewModel.setSelectedCategory(SearchViewModel.SearchType.CATEGORY, null)
            chipCategory.isChecked = false
            chipCategory.isCloseIconVisible = false
        }
        updateChipText(null)
    }

    // Resets a specific chip and clears its selection
    private fun resetChip(chip: Chip) {
        when (chip.id) {
            R.id.chipCountry -> searchViewModel.setSelectedCategory(SearchViewModel.SearchType.COUNTRY, null)
            R.id.chipIngredient -> searchViewModel.setSelectedCategory(SearchViewModel.SearchType.INGREDIENT, null)
            R.id.chipCategory -> searchViewModel.setSelectedCategory(SearchViewModel.SearchType.CATEGORY, null)
        }
        currentSearchType = null
        updateChipText(null)
        chip.isCloseIconVisible = false
        if (etSearch.text.toString().trim().isEmpty()) {
            mealsAdapter.submitList(emptyList())
            noResultsTextView.visibility = View.GONE
        }
        performSearch("")
    }

    // Displays a bottom sheet for selecting search suggestions and handles the result
    private fun showSuggestionsBottomSheet() {
        val currentCountrySelectionBefore = searchViewModel.getSelectedCountry()
        val currentIngredientSelectionBefore = searchViewModel.getSelectedIngredient()
        val currentCategorySelectionBefore = searchViewModel.getSelectedCategory()

        if (!NetworkUtils.isInternetAvailable(requireContext())) {
            showNoInternetDialog()
            return
        }

        val bottomSheet = SuggestionsBottomSheet.newInstance(currentSearchType ?: SearchViewModel.SearchType.COUNTRY) { selectedSuggestion ->
            if (selectedSuggestion != null) {
                when (currentSearchType) {
                    SearchViewModel.SearchType.COUNTRY -> {
                        currentCountrySelection = selectedSuggestion
                        chipCountry.isChecked = true
                        chipCountry.isCloseIconVisible = true
                        searchViewModel.setSelectedCategory(SearchViewModel.SearchType.COUNTRY, selectedSuggestion)
                        updateChipText(selectedSuggestion)
                        Log.d("SearchFragment", "Country selected: $selectedSuggestion, triggering search")
                        progressBar.visibility = View.VISIBLE
                        performSearch("")
                    }
                    SearchViewModel.SearchType.INGREDIENT -> {
                        currentIngredientSelection = selectedSuggestion
                        chipIngredient.isChecked = true
                        chipIngredient.isCloseIconVisible = true
                        searchViewModel.setSelectedCategory(SearchViewModel.SearchType.INGREDIENT, selectedSuggestion)
                        updateChipText(selectedSuggestion)
                        progressBar.visibility = View.VISIBLE
                        performSearch("")
                    }
                    SearchViewModel.SearchType.CATEGORY -> {
                        currentCategorySelection = selectedSuggestion
                        chipCategory.isChecked = true
                        chipCategory.isCloseIconVisible = true
                        searchViewModel.setSelectedCategory(SearchViewModel.SearchType.CATEGORY, selectedSuggestion)
                        updateChipText(selectedSuggestion)
                        progressBar.visibility = View.VISIBLE
                        performSearch("")
                    }
                    else -> {
                        progressBar.visibility = View.GONE
                    }
                }
            } else {
                currentCountrySelection = currentCountrySelectionBefore
                currentIngredientSelection = currentIngredientSelectionBefore
                currentCategorySelection = currentCategorySelectionBefore

                chipCountry.isChecked = currentCountrySelection != null
                chipCountry.isCloseIconVisible = currentCountrySelection != null
                chipCountry.text = currentCountrySelection ?: "Country"

                chipIngredient.isChecked = currentIngredientSelection != null
                chipIngredient.isCloseIconVisible = currentIngredientSelection != null
                chipIngredient.text = currentIngredientSelection ?: "Ingredient"

                chipCategory.isChecked = currentCategorySelection != null
                chipCategory.isCloseIconVisible = currentCategorySelection != null
                chipCategory.text = currentCategorySelection ?: "Category"

                currentSearchType = when {
                    currentCountrySelection != null -> SearchViewModel.SearchType.COUNTRY
                    currentIngredientSelection != null -> SearchViewModel.SearchType.INGREDIENT
                    currentCategorySelection != null -> SearchViewModel.SearchType.CATEGORY
                    else -> null
                }

                if (searchViewModel.hasSelections()) {
                    progressBar.visibility = View.VISIBLE
                    performSearch("")
                } else {
                    progressBar.visibility = View.GONE
                    mealsAdapter.submitList(emptyList())
                    noResultsTextView.visibility = View.GONE
                }
            }
            Log.d("SearchFragment", "Final state: Country=${searchViewModel.getSelectedCountry()}, Ingredient=${searchViewModel.getSelectedIngredient()}, Category=${searchViewModel.getSelectedCategory()}, SearchType=$currentSearchType")
        }
        bottomSheet.show(childFragmentManager, SuggestionsBottomSheet.TAG)
    }

    // Updates the text and state of chips based on current selections
    private fun updateChipText(selectedSuggestion: String?) {
        val countryText = searchViewModel.getSelectedCountry() ?: "Country"
        val ingredientText = searchViewModel.getSelectedIngredient() ?: "Ingredient"
        val categoryText = searchViewModel.getSelectedCategory() ?: "Category"

        chipCountry.text = countryText
        chipCountry.isCloseIconVisible = searchViewModel.getSelectedCountry() != null
        chipCountry.isChecked = searchViewModel.getSelectedCountry() != null

        chipIngredient.text = ingredientText
        chipIngredient.isCloseIconVisible = searchViewModel.getSelectedIngredient() != null
        chipIngredient.isChecked = searchViewModel.getSelectedIngredient() != null

        chipCategory.text = categoryText
        chipCategory.isCloseIconVisible = searchViewModel.getSelectedCategory() != null
        chipCategory.isChecked = searchViewModel.getSelectedCategory() != null
    }

    // Hides the soft keyboard after search input
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        imm?.hideSoftInputFromWindow(etSearch.windowToken, 0)
    }

    private fun goToDetails(id: String) {
        if (isGuest) {
            CreateMaterialAlertDialogBuilder.createGuestLoginDialog(requireContext())
        } else {
            dataViewModel.setItemDetails(id)
            val action = SearchFragmentDirections.actionActionSearchToActionDetails(id)
            findNavController().navigate(action)
        }
    }

    private fun handleAddToPlanClick(meal: Meal) {
        if (isGuest) {
            CreateMaterialAlertDialogBuilder.createGuestLoginDialog(requireContext())
        } else {
            showAddToPlanDialog(meal)
        }
    }

    private fun showAddToPlanDialog(meal: Meal) {
        Log.d("SearchFragment", "Showing day selection dialog for meal: ${meal.strMeal}")
        val days = arrayOf("Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
        AlertDialog.Builder(requireContext())
            .setTitle("What day would you like to add ${meal.strMeal} ?")
            .setItems(days) { _, which ->
                val selectedDay = days[which]
                mealPlanViewModel.addMealToPlan(selectedDay, meal)
                Toast.makeText(requireContext(), "${meal.strMeal} added to $selectedDay", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}