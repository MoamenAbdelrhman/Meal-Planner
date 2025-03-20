package com.example.foodplanner.home.view

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.example.foodplanner.R
import com.example.foodplanner.core.model.local.repository.UserRepositoryImpl
import com.example.foodplanner.core.model.local.source.LocalDataSourceImpl
import com.example.foodplanner.core.model.local.source.UserDatabase
import com.example.foodplanner.core.model.remote.Category
import com.example.foodplanner.core.model.remote.Meal
import com.example.foodplanner.core.model.remote.Response
import com.example.foodplanner.core.model.remote.repository.MealRepositoryImpl
import com.example.foodplanner.core.model.remote.source.RemoteGsonDataImpl
import com.example.foodplanner.core.util.CreateMaterialAlertDialogBuilder
import com.example.foodplanner.core.viewmodel.DataViewModel
import com.example.foodplanner.core.viewmodel.DataViewModelFactory
import com.example.foodplanner.home.view.adapter.AdapterRVCategories
import com.example.foodplanner.home.view.adapter.AdapterRVItemMeal
import com.example.foodplanner.home.viewModel.HomeFragmentViewModel
import com.example.foodplanner.home.viewModel.HomeFragmentViewModelFactory
import com.example.foodplanner.main.viewModel.MainActivityViewModel
import com.example.foodplanner.main.viewModel.MainActivityViewModelFactory
import com.example.foodplanner.meal_plan.viewModel.MealPlanViewModel
import com.example.foodplanner.meal_plan.viewModel.MealPlanViewModelFactory
import com.example.foodplanner.utils.NetworkUtils
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private val homeViewModel: HomeFragmentViewModel by viewModels {
        val remoteGsonDataSource = RemoteGsonDataImpl()
        val mealRepository = MealRepositoryImpl(remoteGsonDataSource)
        val userRepository = UserRepositoryImpl(
            LocalDataSourceImpl(UserDatabase.getDatabaseInstance(requireContext()).userDao()),
            FirebaseAuth.getInstance()
        )
        HomeFragmentViewModelFactory(mealRepository, userRepository)
    }

    private val mainViewModel: MainActivityViewModel by activityViewModels {
        val userRepository = UserRepositoryImpl(
            LocalDataSourceImpl(UserDatabase.getDatabaseInstance(requireContext()).userDao()),
            FirebaseAuth.getInstance()
        )
        MainActivityViewModelFactory(userRepository)
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

    private lateinit var recyclerViewCategories: RecyclerView
    private lateinit var recyclerViewCategoriesItems: RecyclerView
    private lateinit var recyclerViewRecommendations: RecyclerView
    private lateinit var recyclerViewCuisine: RecyclerView
    private lateinit var shimmerMealOfDay: ShimmerFrameLayout
    private lateinit var shimmerCategories: ShimmerFrameLayout
    private lateinit var shimmerCategoriesItem: ShimmerFrameLayout
    private lateinit var shimmerRecommendations: ShimmerFrameLayout
    private lateinit var shimmerCuisine: ShimmerFrameLayout
    private lateinit var btnDrawer: ImageView
    private lateinit var textViewCuisines: TextView
    private lateinit var cardViewFreeTrial: MaterialCardView
    private lateinit var btnAddToPlan: Button
    private lateinit var btnCuisines: MaterialCardView
    private lateinit var popup: PopupMenu
    private lateinit var drawer: DrawerLayout
    private lateinit var searchBarHome: TextView
    private lateinit var mealImage: ImageView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var noInternetImage: ImageView
    private lateinit var nestedScrollView: androidx.core.widget.NestedScrollView

    private var favouriteState = false
    private var isCategoriesLoaded = false
    private var selectedCategory: String? = null // To store the selected category for recyclerview_categories
    private var selectedCuisine: String? = null
    private var isDataLoaded = false
    private var mealOfTheDay: Meal? = null
    private var isInitialLoad = true
    private var hasShownInitialNoInternetDialog = false
    private var isRefreshed = false // To track the refresh state

    private val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_in_right)
        .setPopExitAnim(R.anim.slide_out_right)
        .build()

    private var navController: NavController? = null
    private var recipeId: String = "52772"

    private lateinit var categoriesAdapter: AdapterRVCategories
    private lateinit var categoriesItemAdapter: AdapterRVItemMeal
    private lateinit var recommendationsAdapter: AdapterRVItemMeal
    private lateinit var cuisineAdapter: AdapterRVItemMeal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            if (NetworkUtils.isInternetAvailable(requireContext())) {
                loadInitialData()
                isDataLoaded = true
                isInitialLoad = true
            } else {
                hasShownInitialNoInternetDialog = false
            }
        } else {
            // Restore the state
            isRefreshed = savedInstanceState.getBoolean("isRefreshed", false)
            // Restore selectedCategory only if no refresh was performed
            selectedCategory = if (!isRefreshed) savedInstanceState.getString("selectedCategory", null) else null
            selectedCuisine = savedInstanceState.getString("selectedCuisine", null)
            isInitialLoad = false
            hasShownInitialNoInternetDialog = savedInstanceState.getBoolean("hasShownInitialNoInternetDialog", false)
            isDataLoaded = savedInstanceState.getBoolean("isDataLoaded", false)
        }
    }

    // Loads initial data for the fragment without setting a selected category
    private fun loadInitialData() {
        homeViewModel.getRandomCuisine({ randomCuisine ->
            selectedCuisine = randomCuisine
            homeViewModel.getFilteredMealsByAreas(randomCuisine, requireContext())
        }, requireContext())

        // Load meals based on a random category, but do not set selectedCategory
        homeViewModel.getRandomCategory({ randomCategory ->
            homeViewModel.getFilteredMealsByCategory(randomCategory, requireContext())
        }, requireContext())

        homeViewModel.getRandomMeal(requireContext())
        homeViewModel.getRandomMeals(10, context = requireContext())
        homeViewModel.getCategories(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        initializeUi(view)

        searchBarHome.setOnClickListener {
            mainViewModel.navigateTo(R.id.action_search)
        }

        btnCuisines.setOnClickListener {
            popup.show()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
        checkInternetAndLoadData()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("selectedCategory", selectedCategory) // Save the selected category
        selectedCuisine?.let { outState.putString("selectedCuisine", it) }
        outState.putBoolean("hasShownInitialNoInternetDialog", hasShownInitialNoInternetDialog)
        outState.putBoolean("isDataLoaded", isDataLoaded)
        outState.putBoolean("isRefreshed", isRefreshed) // Save the refresh state
    }

    // Initializes the UI components and sets up the RecyclerViews, adapters, and click listeners
    private fun initializeUi(view: View) {
        recyclerViewCategories = view.findViewById(R.id.recyclerview_categories)
        recyclerViewCategoriesItems = view.findViewById(R.id.recyclerview_mealByCategories)
        recyclerViewRecommendations = view.findViewById(R.id.recyclerview_recommendations)
        recyclerViewCuisine = view.findViewById(R.id.recyclerview_meal_by_fav_cuisine)

        shimmerMealOfDay = view.findViewById(R.id.shimmer_mealofday)
        shimmerCategories = view.findViewById(R.id.shimmer_categories)
        shimmerCategoriesItem = view.findViewById(R.id.shimmer_mealbycategories)
        shimmerRecommendations = view.findViewById(R.id.shimmer_recommendations)
        shimmerCuisine = view.findViewById(R.id.shimmer_meal_by_fav_cuisine)

        cardViewFreeTrial = view.findViewById(R.id.cardViewFreeTrial)
        mealImage = cardViewFreeTrial.findViewById(R.id.meal_image)

        btnAddToPlan = view.findViewById(R.id.btn_add_to_plan)
        btnDrawer = view.findViewById(R.id.btnHomeDrawer)
        textViewCuisines = view.findViewById(R.id.textViewCuisines)

        btnAddToPlan.setOnClickListener {
            mealOfTheDay?.let { meal ->
                showAddToPlanDialog(meal)
            } ?: run {
                Toast.makeText(requireContext(), "No meal available to add", Toast.LENGTH_SHORT).show()
            }
        }

        // Pass selectedCategory to AdapterRVCategories during initialization
        categoriesAdapter = AdapterRVCategories(listOf(), selectedCategory) { name -> goToSearchCategories(name) }
        categoriesItemAdapter = AdapterRVItemMeal(
            listOf(),
            { id -> goToDetails(id) },
            { id, isChange, onComplete -> changeFavouriteState(id, isChange, onComplete) },
            { meal -> showAddToPlanDialog(meal) }
        )
        recommendationsAdapter = AdapterRVItemMeal(
            listOf(),
            { id -> goToDetails(id) },
            { id, isChange, onComplete -> changeFavouriteState(id, isChange, onComplete) },
            { meal -> showAddToPlanDialog(meal) }
        )
        cuisineAdapter = AdapterRVItemMeal(
            listOf(),
            { id -> goToDetails(id) },
            { id, isChange, onComplete -> changeFavouriteState(id, isChange, onComplete) },
            { meal -> showAddToPlanDialog(meal) }
        )
        recyclerViewCategories.adapter = categoriesAdapter
        recyclerViewCategoriesItems.adapter = categoriesItemAdapter
        recyclerViewRecommendations.adapter = recommendationsAdapter
        recyclerViewCuisine.adapter = cuisineAdapter

        setupRecyclerView(recyclerViewCategories)
        setupRecyclerView(recyclerViewCategoriesItems)
        setupRecyclerView(recyclerViewRecommendations)
        setupRecyclerView(recyclerViewCuisine)

        btnCuisines = view.findViewById(R.id.btnCuisines)
        popup = PopupMenu(requireContext(), btnCuisines)
        setupCuisinesPopup()

        drawer = requireActivity().findViewById(R.id.drawer)
        searchBarHome = view.findViewById(R.id.searchBar_home)

        btnDrawer.setOnClickListener {
            drawer.openDrawer(GravityCompat.START)
        }

        navController = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            ?.findNavController()

        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        noInternetImage = view.findViewById(R.id.noInternetImage)
        nestedScrollView = view.findViewById(R.id.nestedScrollView)

        noInternetImage.visibility = View.GONE

        swipeRefreshLayout.setOnRefreshListener {
            hasShownInitialNoInternetDialog = false
            isDataLoaded = false
            checkInternetAndLoadData()
        }
    }

    // Checks internet availability and loads data if available, otherwise shows a no-internet dialog
    private fun checkInternetAndLoadData() {
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            noInternetImage.visibility = View.GONE
            nestedScrollView.visibility = View.VISIBLE
            if (!isDataLoaded) {
                refreshAllData()
                isDataLoaded = true
            }
        } else {
            noInternetImage.visibility = View.VISIBLE
            nestedScrollView.visibility = View.GONE
            swipeRefreshLayout.isRefreshing = false
            stopAllShimmers()
            if (!hasShownInitialNoInternetDialog) {
                showNoInternetDialog()
                hasShownInitialNoInternetDialog = false
            }
        }
    }

    // Displays a dialog to inform the user of no internet connection with retry and cancel options
    private fun showNoInternetDialog() {
        CreateMaterialAlertDialogBuilder.createMaterialAlertDialogBuilderOkCancel(
            context = requireContext(),
            title = "No Internet Connection",
            message = "Please check your internet connection and try again.",
            positiveBtnMsg = "Retry",
            negativeBtnMsg = "Cancel",
            positiveBtnFun = {
                checkInternetAndLoadData()
            }
        )
    }

    // Stops all shimmer effects and hides them
    private fun stopAllShimmers() {
        shimmerMealOfDay.stopShimmer()
        shimmerMealOfDay.visibility = View.GONE
        shimmerCategories.stopShimmer()
        shimmerCategories.visibility = View.GONE
        shimmerCategoriesItem.stopShimmer()
        shimmerCategoriesItem.visibility = View.GONE
        shimmerRecommendations.stopShimmer()
        shimmerRecommendations.visibility = View.GONE
        shimmerCuisine.stopShimmer()
        shimmerCuisine.visibility = View.GONE
    }

    // Refreshes all data in the fragment, resetting selections and reloading content
    private fun refreshAllData() {
        isDataLoaded = false
        isInitialLoad = true
        selectedCategory = null
        selectedCuisine = null
        isRefreshed = true

        // Reset categorySearch in dataViewModel to avoid using an old value
        dataViewModel.updateSearchCategory(null)

        categoriesAdapter.resetSelection()

        categoriesAdapter.submitList(emptyList(), null)
        categoriesItemAdapter.submitList(emptyList())
        recommendationsAdapter.submitList(emptyList())
        cuisineAdapter.submitList(emptyList())

        shimmerMealOfDay.visibility = View.VISIBLE
        shimmerMealOfDay.startShimmer()

        shimmerCategories.visibility = View.VISIBLE
        shimmerCategories.startShimmer()

        shimmerCategoriesItem.visibility = View.VISIBLE
        shimmerCategoriesItem.startShimmer()

        shimmerRecommendations.visibility = View.VISIBLE
        shimmerRecommendations.startShimmer()

        shimmerCuisine.visibility = View.VISIBLE
        shimmerCuisine.startShimmer()

        btnAddToPlan.isEnabled = false

        homeViewModel.getRandomMeal(requireContext())
        homeViewModel.getRandomMeals(10, context = requireContext())
        homeViewModel.getCategories(requireContext())

        // Load meals based on a random category, but do not set selectedCategory
        homeViewModel.getRandomCategory({ randomCategory ->
            homeViewModel.getFilteredMealsByCategory(randomCategory, requireContext())
        }, requireContext())

        homeViewModel.getRandomCuisine({ randomCuisine ->
            selectedCuisine = randomCuisine
            homeViewModel.getFilteredMealsByAreas(randomCuisine, requireContext())
        }, requireContext())

        swipeRefreshLayout.isRefreshing = false
    }


    // Sets up observers for LiveData objects to update the UI based on data changes
    private fun initObservers() {

        // Observes the favorite state of a meal and updates the local favouriteState variable
        dataViewModel.isFavourite.observe(viewLifecycleOwner, Observer { isFavourite ->
            favouriteState = isFavourite
        })

        // Observes the selected category for search and updates the UI if it's the initial load and no refresh has occurred
        dataViewModel.categorySearch.observe(viewLifecycleOwner) { category ->
            category?.let {
                // Update selectedCategory only if no refresh was performed and this is the initial load
                if (isInitialLoad && !isRefreshed) {
                    selectedCategory = it
                    homeViewModel.getFilteredMealsByCategory(it, requireContext())
                    isInitialLoad = false
                }
            }
        }

        with(dataViewModel) {
            // Observes the list of cuisines and populates the popup menu with the available cuisines
            cuisinesData.observe(viewLifecycleOwner) { data ->
                data?.forEachIndexed { index, item ->
                    popup.menu.add(0, index, 0, item)
                }
            }
        }

        with(homeViewModel) {
            // Observes the random meal of the day and updates the free trial card with the meal details
            randomMeal.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Response.Loading -> {
                        shimmerMealOfDay.visibility = View.VISIBLE
                        shimmerMealOfDay.startShimmer()
                        cardViewFreeTrial.visibility = View.INVISIBLE
                        btnAddToPlan.isEnabled = false
                    }
                    is Response.Success -> {
                        shimmerMealOfDay.stopShimmer()
                        shimmerMealOfDay.visibility = View.GONE
                        cardViewFreeTrial.visibility = View.VISIBLE
                        val meal = response.data
                        recipeId = meal.idMeal
                        updateFreeTrialCard(meal)
                        mealImage.setOnClickListener {
                            goToDetails(recipeId)
                        }
                        btnAddToPlan.isEnabled = true
                    }
                    is Response.Failure -> {
                        getRandomMeal(requireContext())
                    }
                }
            }

            // Observes the list of categories and updates the categories RecyclerView with the fetched data
            dataCategories.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Response.Loading -> {
                        categoriesAdapter.submitList(emptyList(), selectedCategory)
                        shimmerCategories.visibility = View.VISIBLE
                        shimmerCategories.startShimmer()
                    }
                    is Response.Success -> {
                        shimmerCategories.stopShimmer()
                        shimmerCategories.visibility = View.GONE
                        categoriesAdapter.submitList(response.data.categories, selectedCategory)
                        isCategoriesLoaded = true
                    }
                    is Response.Failure -> {
                        getCategories(requireContext())
                    }
                }
            }

            // Observes meals filtered by category and updates the meals RecyclerView for the selected category
            filteredMealsByCategory.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Response.Loading -> {
                        categoriesItemAdapter.submitList(emptyList())
                        shimmerCategoriesItem.visibility = View.VISIBLE
                        shimmerCategoriesItem.startShimmer()
                    }
                    is Response.Success -> {
                        shimmerCategoriesItem.stopShimmer()
                        shimmerCategoriesItem.visibility = View.GONE
                        if (response.data.meals.isNotEmpty()) {
                            categoriesItemAdapter.submitList(response.data.meals)
                        }
                    }
                    is Response.Failure -> {
                        // Load meals based on a random category, but do not set selectedCategory
                        homeViewModel.getRandomCategory( { randomCategory ->
                            homeViewModel.getFilteredMealsByCategory(randomCategory, requireContext())
                        }, requireContext())
                    }
                }
            }

            // Observes recommended meals and updates the recommendations RecyclerView with the fetched meals
            someRecommendedMeals.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Response.Loading -> {
                        recommendationsAdapter.submitList(emptyList())
                        shimmerRecommendations.visibility = View.VISIBLE
                        shimmerRecommendations.startShimmer()
                    }
                    is Response.Success -> {
                        shimmerRecommendations.stopShimmer()
                        shimmerRecommendations.visibility = View.GONE
                        recommendationsAdapter.submitList(response.data)
                    }
                    is Response.Failure -> {
                        getRandomMeals(10, context = requireContext())
                    }
                }
            }

            // Observes meals filtered by area (cuisine) and updates the cuisine RecyclerView with the fetched meals
            filteredMealsByAreas.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Response.Loading -> {
                        cuisineAdapter.submitList(emptyList())
                        shimmerCuisine.visibility = View.VISIBLE
                        shimmerCuisine.startShimmer()
                    }
                    is Response.Success -> {
                        shimmerCuisine.stopShimmer()
                        shimmerCuisine.visibility = View.GONE
                        if (response.data.meals.isNotEmpty()) {
                            cuisineAdapter.submitList(response.data.meals)
                            textViewCuisines.text = "$selectedCuisine Meals"
                        }
                    }
                    is Response.Failure -> {
                        homeViewModel.getRandomCuisine( { randomCuisine ->
                            selectedCuisine = randomCuisine
                            getFilteredMealsByAreas(randomCuisine, requireContext())
                        },requireContext())
                    }
                }
            }
        }
    }

    // Configures a RecyclerView with a horizontal LinearLayoutManager
    private fun setupRecyclerView(recyclerView: RecyclerView) {
        recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }

    // Navigates to the details screen for a specific meal
    private fun goToDetails(id: String) {
        dataViewModel.setItemDetails(id)
        navController?.navigate(R.id.action_details, null, navOptions)
    }

    // Handles category selection, updates the selected category, and refreshes the meals list
    private fun goToSearchCategories(name: String) {
        if (selectedCategory != name) {
            selectedCategory = name
            dataViewModel.updateSearchCategory(name)
            homeViewModel.getFilteredMealsByCategory(name, requireContext())
            // Update the Adapter with the newly selected category
            categoriesAdapter.submitList(categoriesAdapter.getCurrentList(), selectedCategory)
            isRefreshed = false
        }
    }

    // Changes the favorite state of a meal and notifies the caller of the result
    private fun changeFavouriteState(
        recipeId: String,
        isChange: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        dataViewModel.viewModelScope.launch {
            dataViewModel.changeFavouriteState(recipeId, isChange)
        }.invokeOnCompletion {
            onComplete(favouriteState)
        }
    }

    // Sets up the popup menu for cuisines and handles cuisine selection
    private fun setupCuisinesPopup() {
        homeViewModel.getAllCuisines(requireContext())

        homeViewModel.allCuisines.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Response.Loading -> {
                    Log.d("HomeFragment", "Loading cuisines...")
                }
                is Response.Success -> {
                    response.data.let { cuisines ->
                        popup.menu.clear()
                        cuisines.forEachIndexed { index, cuisine ->
                            popup.menu.add(0, index, 0, cuisine)
                        }
                    }
                }
                is Response.Failure -> {
                }
            }
        }

        btnCuisines.setOnClickListener {
            popup.show()
        }

        popup.setOnMenuItemClickListener { item ->
            val selectedCuisineNew = item.title.toString()
            selectedCuisine = selectedCuisineNew
            homeViewModel.getFilteredMealsByAreas(selectedCuisineNew, requireContext())
            textViewCuisines.text = "$selectedCuisineNew Meals"
            true
        }
    }

    // Updates the free trial card with the meal of the day
    private fun updateFreeTrialCard(meal: Meal?) {
        meal?.let {
            val textViewMealName = cardViewFreeTrial.findViewById<TextView>(R.id.meal_name)
            textViewMealName.text = it.strMeal

            val imageViewMeal = cardViewFreeTrial.findViewById<ImageView>(R.id.meal_image)
            Glide.with(requireContext())
                .load(it.strMealThumb)
                .into(imageViewMeal)

            mealOfTheDay = it
        }
    }

    // Shows a dialog to add a meal to a specific day in the meal plan
    private fun showAddToPlanDialog(meal: Meal) {
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

    // Helper function to get the current list from categoriesAdapter
    private fun AdapterRVCategories.getCurrentList(): List<Category> {
        return this.javaClass.getDeclaredField("categories").let { field ->
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            field.get(this) as List<Category>
        }
    }
}