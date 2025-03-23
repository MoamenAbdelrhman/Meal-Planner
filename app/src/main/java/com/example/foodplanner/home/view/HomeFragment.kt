package com.example.foodplanner.home.view

import android.app.AlertDialog
import android.content.Intent
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
import com.example.foodplanner.auth.AuthActivity
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
    private var selectedCategory: String? = null
    private var selectedCuisine: String? = null
    private var isDataLoaded = false
    private var mealOfTheDay: Meal? = null
    private var isInitialLoad = true
    private var hasShownInitialNoInternetDialog = false
    private var isRefreshed = false
    private var isGuest: Boolean = false

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
        isGuest = requireActivity().intent.getBooleanExtra("IS_GUEST", false)
        if (savedInstanceState == null) {
            if (NetworkUtils.isInternetAvailable(requireContext())) {
                loadInitialData()
                isDataLoaded = true
                isInitialLoad = true
            } else {
                hasShownInitialNoInternetDialog = false
            }
        } else {
            isRefreshed = savedInstanceState.getBoolean("isRefreshed", false)
            selectedCategory = if (!isRefreshed) savedInstanceState.getString("selectedCategory", null) else null
            selectedCuisine = savedInstanceState.getString("selectedCuisine", null)
            isInitialLoad = false
            hasShownInitialNoInternetDialog = savedInstanceState.getBoolean("hasShownInitialNoInternetDialog", false)
            isDataLoaded = savedInstanceState.getBoolean("isDataLoaded", false)
        }
    }

    private fun loadInitialData() {
        homeViewModel.getRandomCuisine({ randomCuisine ->
            selectedCuisine = randomCuisine
            homeViewModel.getFilteredMealsByAreas(randomCuisine, requireContext())
        }, requireContext())

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
        searchBarHome.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_search)
        }
        initObservers()
        checkInternetAndLoadData()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("selectedCategory", selectedCategory)
        selectedCuisine?.let { outState.putString("selectedCuisine", it) }
        outState.putBoolean("hasShownInitialNoInternetDialog", hasShownInitialNoInternetDialog)
        outState.putBoolean("isDataLoaded", isDataLoaded)
        outState.putBoolean("isRefreshed", isRefreshed)
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
            if (isGuest) {
                CreateMaterialAlertDialogBuilder.createGuestLoginDialog(requireContext())
            } else {
                mealOfTheDay?.let { meal ->
                    showAddToPlanDialog(meal)
                } ?: run {
                    Toast.makeText(requireContext(), "No meal available to add", Toast.LENGTH_SHORT).show()
                }
            }
        }

        categoriesAdapter = AdapterRVCategories(listOf(), selectedCategory) { name -> goToSearchCategories(name) }
        categoriesItemAdapter = AdapterRVItemMeal(
            listOf(),
            { id -> goToDetails(id) },
            { id, isChange, onComplete -> changeFavouriteState(id, isChange, onComplete) },
            { meal ->
                if (isGuest) {
                    CreateMaterialAlertDialogBuilder.createGuestLoginDialog(requireContext())
                } else {
                    showAddToPlanDialog(meal)
                }
            }
        )
        recommendationsAdapter = AdapterRVItemMeal(
            listOf(),
            { id -> goToDetails(id) },
            { id, isChange, onComplete -> changeFavouriteState(id, isChange, onComplete) },
            { meal ->
                if (isGuest) {
                    CreateMaterialAlertDialogBuilder.createGuestLoginDialog(requireContext())
                } else {
                    showAddToPlanDialog(meal)
                }
            }
        )
        cuisineAdapter = AdapterRVItemMeal(
            listOf(),
            { id -> goToDetails(id) },
            { id, isChange, onComplete -> changeFavouriteState(id, isChange, onComplete) },
            { meal ->
                if (isGuest) {
                    CreateMaterialAlertDialogBuilder.createGuestLoginDialog(requireContext())
                } else {
                    showAddToPlanDialog(meal)
                }
            }
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

    private fun refreshAllData() {
        isDataLoaded = false
        isInitialLoad = true
        selectedCategory = null
        selectedCuisine = null
        isRefreshed = true

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

        homeViewModel.getRandomCategory({ randomCategory ->
            homeViewModel.getFilteredMealsByCategory(randomCategory, requireContext())
        }, requireContext())

        homeViewModel.getRandomCuisine({ randomCuisine ->
            selectedCuisine = randomCuisine
            homeViewModel.getFilteredMealsByAreas(randomCuisine, requireContext())
        }, requireContext())

        swipeRefreshLayout.isRefreshing = false
    }

    private fun initObservers() {
        dataViewModel.isFavourite.observe(viewLifecycleOwner, Observer { isFavourite ->
            favouriteState = isFavourite
        })

        dataViewModel.categorySearch.observe(viewLifecycleOwner) { category ->
            category?.let {
                if (isInitialLoad && !isRefreshed) {
                    selectedCategory = it
                    homeViewModel.getFilteredMealsByCategory(it, requireContext())
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
                        homeViewModel.getRandomCategory({ randomCategory ->
                            homeViewModel.getFilteredMealsByCategory(randomCategory, requireContext())
                        }, requireContext())
                    }
                }
            }

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
                        homeViewModel.getRandomCuisine({ randomCuisine ->
                            selectedCuisine = randomCuisine
                            getFilteredMealsByAreas(randomCuisine, requireContext())
                        }, requireContext())
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
            homeViewModel.getFilteredMealsByCategory(name, requireContext())
            categoriesAdapter.submitList(categoriesAdapter.getCurrentList(), selectedCategory)
            isRefreshed = false
        }
    }

    private fun changeFavouriteState(
        recipeId: String,
        isChange: Boolean,
        onComplete: (Boolean, Boolean) -> Unit
    ) {
        if (isGuest && isChange) {
            CreateMaterialAlertDialogBuilder.createGuestLoginDialog(requireContext())
            onComplete(false, true)
            return
        }


        val isCurrentlyFavourite = dataViewModel.meals.value?.any { it.idMeal == recipeId } == true

        if (isChange && isCurrentlyFavourite) {
            CreateMaterialAlertDialogBuilder.createConfirmRemovalDialog(
                context = requireContext(),
                message = "Are you sure you want to remove this meal from your favorites?",
                positiveAction = {
                    dataViewModel.viewModelScope.launch {
                        dataViewModel.changeFavouriteState(recipeId, true)
                    }.invokeOnCompletion {
                        onComplete(dataViewModel.isFavourite.value ?: false, false)
                    }
                },
                negativeAction = {
                    onComplete(dataViewModel.isFavourite.value ?: true, true)
                }
            )
        } else {
            dataViewModel.viewModelScope.launch {
                dataViewModel.changeFavouriteState(recipeId, isChange)
            }.invokeOnCompletion {
                onComplete(dataViewModel.isFavourite.value ?: false, false)
            }
        }
    }

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
                is Response.Failure -> {}
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

    private fun AdapterRVCategories.getCurrentList(): List<Category> {
        return this.javaClass.getDeclaredField("categories").let { field ->
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            field.get(this) as List<Category>
        }
    }
}