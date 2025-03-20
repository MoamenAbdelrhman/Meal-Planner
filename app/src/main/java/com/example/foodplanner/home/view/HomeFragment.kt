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
import androidx.constraintlayout.widget.ConstraintLayout
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
import com.example.foodplanner.core.model.remote.Meal
import com.example.foodplanner.core.model.remote.Response
import com.example.foodplanner.core.model.remote.repository.MealRepositoryImpl
import com.example.foodplanner.core.model.remote.source.RemoteGsonDataImpl
import com.example.foodplanner.core.util.CreateMaterialAlertDialogBuilder.createFailureResponse
import com.example.foodplanner.core.util.CreateMaterialAlertDialogBuilder.createMaterialAlertDialogBuilderOk
import com.example.foodplanner.core.util.CreateMaterialAlertDialogBuilder.createMaterialAlertDialogBuilderOkCancel
import com.example.foodplanner.core.viewmodel.DataViewModel
import com.example.foodplanner.core.viewmodel.DataViewModelFactory
import com.example.foodplanner.details.viewmodel.DetailsFactory
import com.example.foodplanner.details.viewmodel.DetailsViewModel
import com.example.foodplanner.home.view.adapter.AdapterRVCategories
import com.example.foodplanner.home.view.adapter.AdapterRVItemMeal
import com.example.foodplanner.home.viewModel.HomeFragmentViewModel
import com.example.foodplanner.home.viewModel.HomeFragmentViewModelFactory
import com.example.foodplanner.main.viewModel.RecipeActivityViewModel
import com.example.foodplanner.main.viewModel.RecipeActivityViewModelFactory
import com.example.foodplanner.meal_plan.viewModel.MealPlanViewModel
import com.example.foodplanner.meal_plan.viewModel.MealPlanViewModelFactory
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

    private val recipeViewModel: RecipeActivityViewModel by activityViewModels {
        val userRepository = UserRepositoryImpl(
            LocalDataSourceImpl(UserDatabase.getDatabaseInstance(requireContext()).userDao()),
            FirebaseAuth.getInstance()
        )
        RecipeActivityViewModelFactory(userRepository)
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

    private var favouriteState = false
    private var isCategoriesLoaded = false
    private var selectedCategory: String? = null
    private var selectedCuisine: String? = null
    private var isDataLoaded = false
    private var mealOfTheDay: Meal? = null
    private var isInitialLoad = true

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
            homeViewModel.getRandomCuisine { randomCuisine ->
                selectedCuisine = randomCuisine
                homeViewModel.getFilteredMealsByAreas(randomCuisine)
            }

            homeViewModel.getRandomCategory { randomCategory ->
                selectedCategory = randomCategory
                homeViewModel.getFilteredMealsByCategory(randomCategory)
            }

            homeViewModel.getRandomMeal()
            homeViewModel.getRandomMeals(10)
            homeViewModel.getCategories()
            isDataLoaded = true
            isInitialLoad = true
        } else {
            selectedCategory = savedInstanceState.getString("selectedCategory", null)
            selectedCuisine = savedInstanceState.getString("selectedCuisine", null)
            isInitialLoad = false

            selectedCuisine?.let { homeViewModel.getFilteredMealsByAreas(it) }
            selectedCategory?.let { homeViewModel.getFilteredMealsByCategory(it) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        initializeUi(view)
        searchBarHome.setOnClickListener {
            recipeViewModel.navigateTo(R.id.action_search)
        }

        btnCuisines.setOnClickListener {
            popup.show()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedCategory?.let {
            outState.putString("selectedCategory", it)
        }
        selectedCuisine?.let {
            outState.putString("selectedCuisine", it)
        }
    }

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

        categoriesAdapter = AdapterRVCategories(listOf()) { name -> goToSearchCategories(name) }
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
        swipeRefreshLayout.setOnRefreshListener {
            refreshAllData()
        }
    }

    private fun refreshAllData() {
        isDataLoaded = false
        isInitialLoad = true
        selectedCategory = null
        selectedCuisine = null

        // Clear the RecyclerView adapters
        categoriesAdapter.submitList(emptyList())
        categoriesItemAdapter.submitList(emptyList())
        recommendationsAdapter.submitList(emptyList())
        cuisineAdapter.submitList(emptyList())

        // Reset and show all Shimmer effects
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

        // Disable the "Add to Plan" button while loading
        btnAddToPlan.isEnabled = false

        // Reload data
        homeViewModel.getRandomMeal()
        homeViewModel.getRandomMeals(10)
        homeViewModel.getCategories()

        homeViewModel.getRandomCategory { randomCategory ->
            selectedCategory = randomCategory
            homeViewModel.getFilteredMealsByCategory(randomCategory)
        }

        homeViewModel.getRandomCuisine { randomCuisine ->
            selectedCuisine = randomCuisine
            homeViewModel.getFilteredMealsByAreas(randomCuisine)
        }

        // Stop the SwipeRefreshLayout spinner
        swipeRefreshLayout.isRefreshing = false
    }

    private fun initObservers() {
        dataViewModel.isFavourite.observe(viewLifecycleOwner, Observer { isFavourite ->
            favouriteState = isFavourite
        })

        dataViewModel.categorySearch.observe(viewLifecycleOwner) { category ->
            category?.let {
                if (isInitialLoad) {
                    selectedCategory = it
                    homeViewModel.getFilteredMealsByCategory(it)
                    isInitialLoad = false
                }
            }
        }

        with(dataViewModel) {
            cuisinesData.observe(viewLifecycleOwner) { data ->
                data?.forEachIndexed { index, item ->
                    popup.menu.add(0, index, 0, item)
                }
            }
        }

        with(homeViewModel) {
            randomMeal.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Response.Loading -> {
                        // Ensure Shimmer is visible and running
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
                        getRandomMeal()
                    }
                }
            }

            dataCategories.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Response.Loading -> {
                        // Ensure Shimmer is visible and running
                        shimmerCategories.visibility = View.VISIBLE
                        shimmerCategories.startShimmer()
                    }
                    is Response.Success -> {
                        shimmerCategories.stopShimmer()
                        shimmerCategories.visibility = View.GONE
                        categoriesAdapter.submitList(response.data.categories)
                        isCategoriesLoaded = true
                    }
                    is Response.Failure -> {
                        getCategories()
                    }
                }
            }

            filteredMealsByCategory.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Response.Loading -> {
                        // Ensure Shimmer is visible and running
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
                        homeViewModel.getRandomCategory { randomCategory ->
                            selectedCategory = randomCategory
                            homeViewModel.getFilteredMealsByCategory(randomCategory)
                        }
                    }
                }
            }

            someRecommendedMeals.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Response.Loading -> {
                        // Ensure Shimmer is visible and running
                        shimmerRecommendations.visibility = View.VISIBLE
                        shimmerRecommendations.startShimmer()
                    }
                    is Response.Success -> {
                        shimmerRecommendations.stopShimmer()
                        shimmerRecommendations.visibility = View.GONE
                        recommendationsAdapter.submitList(response.data)
                    }
                    is Response.Failure -> {
                        getRandomMeals(10)
                    }
                }
            }

            filteredMealsByAreas.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Response.Loading -> {
                        // Ensure Shimmer is visible and running
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
                        homeViewModel.getRandomCuisine { randomCuisine ->
                            selectedCuisine = randomCuisine
                            homeViewModel.getFilteredMealsByAreas(randomCuisine)
                        }
                    }
                }
            }
        }
    }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }

    private fun goToDetails(id: String) {
        dataViewModel.setItemDetails(id)
        navController?.navigate(R.id.action_details, null, navOptions)
    }

    private fun goToSearchCategories(name: String) {
        if (selectedCategory != name) {
            selectedCategory = name
            dataViewModel.updateSearchCategory(name)
            homeViewModel.getFilteredMealsByCategory(name)
        }
    }

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

    private fun setupCuisinesPopup() {
        homeViewModel.getAllCuisines()

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
                    Log.e("HomeFragment", "Failed to load cuisines")
                }
            }
        }

        btnCuisines.setOnClickListener {
            popup.show()
        }

        popup.setOnMenuItemClickListener { item ->
            val selectedCuisineNew = item.title.toString()
            selectedCuisine = selectedCuisineNew
            homeViewModel.getFilteredMealsByAreas(selectedCuisineNew)
            textViewCuisines.text = "$selectedCuisineNew Meals"
            true
        }
    }

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
}