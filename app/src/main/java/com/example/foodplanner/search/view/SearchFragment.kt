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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isGuest = FirebaseAuth.getInstance().currentUser == null ||
                requireActivity().intent.getBooleanExtra("IS_GUEST", false)
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
        noResultsTextView = view.findViewById(R.id.noResultsTextView)

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

    private fun setupSearchListener() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    progressBar.visibility = View.VISIBLE
                    performSearch(query)
                } else {
                    if (currentSearchType != null && (currentCountrySelection != null || currentIngredientSelection != null || currentCategorySelection != null)) {
                        progressBar.visibility = View.VISIBLE
                        performSearch("")
                    } else {
                        searchViewModel.resetSelections()
                        mealsAdapter.submitList(emptyList())
                        progressBar.visibility = View.GONE
                        noResultsTextView.visibility = View.GONE
                    }
                }
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        chipCountry.setOnClickListener {
            resetAllSelectionsExcept(SearchViewModel.SearchType.COUNTRY)
            currentSearchType = SearchViewModel.SearchType.COUNTRY
            showSuggestionsBottomSheet()
        }

        chipIngredient.setOnClickListener {
            resetAllSelectionsExcept(SearchViewModel.SearchType.INGREDIENT)
            currentSearchType = SearchViewModel.SearchType.INGREDIENT
            showSuggestionsBottomSheet()
        }

        chipCategory.setOnClickListener {
            resetAllSelectionsExcept(SearchViewModel.SearchType.CATEGORY)
            currentSearchType = SearchViewModel.SearchType.CATEGORY
            showSuggestionsBottomSheet()
        }

        chipCountry.setOnCloseIconClickListener {
            resetChip(chipCountry)
            if (etSearch.text.toString().trim().isEmpty()) {
                noResultsTextView.visibility = View.GONE
            }
            performSearch("")
        }
        chipIngredient.setOnCloseIconClickListener {
            resetChip(chipIngredient)
            if (etSearch.text.toString().trim().isEmpty()) {
                noResultsTextView.visibility = View.GONE
            }
            performSearch("")
        }
        chipCategory.setOnCloseIconClickListener {
            resetChip(chipCategory)
            if (etSearch.text.toString().trim().isEmpty()) {
                noResultsTextView.visibility = View.GONE
            }
            performSearch("")
        }
    }

    private fun performSearchWithInternetCheck(query: String) {
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            progressBar.visibility = View.VISIBLE
            performSearch(query)
        } else {
            progressBar.visibility = View.GONE
            showNoInternetDialog()
        }
    }

    private fun showNoInternetDialog() {
        CreateMaterialAlertDialogBuilder.createMaterialAlertDialogBuilderOk(
            context = requireContext(),
            title = "No Internet Connection",
            message = "Please connect to the internet to search for meals.",
            positiveBtnMsg = "OK",
            positiveBtnFun = {}
        )
    }

    private fun performSearch(query: String) {
        searchViewModel.searchMealsByName(query, currentSearchType)
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
                when (currentSearchType) {
                    SearchViewModel.SearchType.COUNTRY -> {
                        if (previousCountrySelection != null) {
                            currentCountrySelection = previousCountrySelection
                            chipCountry.isChecked = true
                            chipCountry.isCloseIconVisible = true
                            chipCountry.text = previousCountrySelection
                            searchViewModel.setSelectedCategory(SearchViewModel.SearchType.COUNTRY, previousCountrySelection)
                            progressBar.visibility = View.VISIBLE
                            performSearch("")
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
                            progressBar.visibility = View.VISIBLE
                            performSearch("")
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
                            progressBar.visibility = View.VISIBLE
                            performSearch("")
                        } else {
                            currentCategorySelection = null
                            chipCategory.isChecked = false
                            chipCategory.isCloseIconVisible = false
                            chipCategory.text = "Category"
                            searchViewModel.resetSelections()
                            mealsAdapter.submitList(emptyList())
                        }
                    }
                    else -> {
                    }
                }
                progressBar.visibility = View.GONE
            }
        }
        bottomSheet.show(childFragmentManager, SuggestionsBottomSheet.TAG)
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
        val action = SearchFragmentDirections.actionSearchToDetails(id)
        findNavController().navigate(action)
    }

    private fun handleAddToPlanClick(meal: Meal) {
        if (isGuest) {
            showGuestRestrictionDialog("Guests cannot add meals to the plan. Please log in.")
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

    private fun showGuestRestrictionDialog(message: String) {
        CreateMaterialAlertDialogBuilder.createMaterialAlertDialogBuilderOkCancel(
            context = requireContext(),
            title = "Restricted Action",
            message = message,
            positiveBtnMsg = "Log In",
            negativeBtnMsg = "Cancel",
            positiveBtnFun = {
                findNavController().navigate(R.id.action_search_to_authActivity)
            }
        )
    }
}