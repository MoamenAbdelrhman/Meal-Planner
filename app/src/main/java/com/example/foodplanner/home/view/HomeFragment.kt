package com.example.foodplanner.home.view

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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.example.foodplanner.core.util.SystemChecks
import com.example.foodplanner.core.viewmodel.DataViewModel
import com.example.foodplanner.core.viewmodel.DataViewModelFactory
import com.example.foodplanner.home.view.adapter.AdapterRVCategories
import com.example.foodplanner.home.view.adapter.AdapterRVItemMeal
import com.example.foodplanner.home.viewModel.HomeFragmentViewModel
import com.example.foodplanner.home.viewModel.HomeFragmentViewModelFactory
import com.example.foodplanner.main.viewModel.RecipeActivityViewModel
import com.example.foodplanner.main.viewModel.RecipeActivityViewModelFactory
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth


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
        DataViewModelFactory(userRepository, mealRepository)
    }

    private lateinit var recyclerViewCategories: RecyclerView
    private lateinit var recyclerViewCategoriesItems: RecyclerView
    private lateinit var recyclerViewRecommendations: RecyclerView
    private lateinit var recyclerViewCuisine: RecyclerView
    private lateinit var shimmerCategories: ShimmerFrameLayout
    private lateinit var shimmerCategoriesItem: ShimmerFrameLayout
    private lateinit var shimmerRecommendations: ShimmerFrameLayout
    private lateinit var shimmerCuisine: ShimmerFrameLayout
    private lateinit var btnDrawer: ImageView
    private lateinit var textViewCuisines: TextView
    private lateinit var constraintLayoutGold: ConstraintLayout
    private lateinit var cardViewFreeTrial: MaterialCardView
    private lateinit var shimmerGold: ShimmerFrameLayout
    private lateinit var recyclerViewGold: RecyclerView
    private lateinit var btnAddToPlan: Button
    private lateinit var btnCuisines: MaterialCardView
    private lateinit var popup: PopupMenu
    private lateinit var drawer: DrawerLayout
    private lateinit var searchBarHome: TextView

    private val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_in_right)
        .setPopExitAnim(R.anim.slide_out_right)
        .build()

    private var navController: NavController? = null
    private var chosenCuisine: String? = null
    private var chosenCategories: String? = null

    private lateinit var categoriesAdapter: AdapterRVCategories
    private lateinit var categoriesItemAdapter: AdapterRVItemMeal
    private lateinit var recommendationsAdapter: AdapterRVItemMeal
    private lateinit var cuisineAdapter: AdapterRVItemMeal
    private lateinit var goldAdapter: AdapterRVItemMeal

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        initializeUi(view)
        checkConnection()
        loadDefaultMeals()

        homeViewModel.getFilteredMealsByAreas("Egyptian")
        textViewCuisines.text = "Egyptian Recipes for you"

        homeViewModel.getRandomMeal()

        searchBarHome.setOnClickListener {
            recipeViewModel.navigateTo(R.id.nav_search)
        }

        btnCuisines.setOnClickListener {
            popup.show()
        }

        return view
    }

    private fun initObservers() {

        dataViewModel.categorySearch.observe(viewLifecycleOwner) { category ->
            category?.let {
                homeViewModel.getFilteredMealsByCategory(it)
            }
        }

        /*with(dataViewModel) {
            mainCuisine.observe(viewLifecycleOwner) { mainCuisine ->
                mainCuisine?.let { homeFavCuisines(it) }
            }
        }

        with(dataViewModel) {
            isSubscribed.observe(viewLifecycleOwner) { subscribed ->
                subscribed?.let {
                    when (it) {
                        true -> {
                            cardViewFreeTrial.visibility = View.GONE
                            constraintLayoutGold.visibility = View.VISIBLE
                        }
                        false -> {
                            cardViewFreeTrial.visibility = View.VISIBLE
                            constraintLayoutGold.visibility = View.GONE
                        }
                    }
                }
            }
        }*/

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
                        // إظهار مؤشر تحميل إذا لزم الأمر
                    }
                    is Response.Success -> {
                        val meal = response.data
                        updateFreeTrialCard(meal)
                    }
                    is Response.Failure -> {
                        // التعامل مع الفشل
                        Log.e("HomeFragment", "Failed to load random meal")
                    }
                }
            }
        }

        // Handle categories
        with(homeViewModel) {
            getCategories()
            dataCategories.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Response.Loading -> shimmerCategories.startShimmer()
                    is Response.Success -> {
                        shimmerCategories.stopShimmer()
                        shimmerCategories.visibility = View.GONE
                        categoriesAdapter.submitList(response.data.categories)
                    }
                    is Response.Failure -> createFailureResponse(response, requireContext()) {
                        getCategories()
                    }
                }
            }
        }
        // Handle random meals for categories
        with(homeViewModel) {
            defaultMeals.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Response.Loading -> shimmerCategoriesItem.startShimmer()
                    is Response.Success -> {
                        shimmerCategoriesItem.stopShimmer()
                        shimmerCategoriesItem.visibility = View.GONE
                        Log.d("HomeFragment", "Default Meals: ${response.data}")
                        if (response.data.isNotEmpty()) {
                            categoriesItemAdapter.submitList(response.data)
                        } else {
                            Log.d("HomeFragment", "No default meals found")
                        }
                    }
                    is Response.Failure -> {
                        createFailureResponse(response, requireContext()) {
                            loadDefaultMeals()
                        }
                        Log.d("HomeFragment", "Failed to load default meals")
                    }
                }
            }
        }
        // Handle filter meals for categories
        with(homeViewModel) {
            Log.d("HomeFragment", "Filtered Meals")
            filteredMealsByCategory.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Response.Loading -> shimmerCategoriesItem.startShimmer()
                    is Response.Success -> {
                        shimmerCategoriesItem.stopShimmer()
                        shimmerCategoriesItem.visibility = View.GONE
                        Log.d("HomeFragment", "Filtered Meals: ${response.data.meals}")
                        if (response.data.meals.isNotEmpty()) {
                            categoriesItemAdapter.submitList(response.data.meals)
                        } else {
                            Log.d("HomeFragment", "No meals found for this category")
                        }
                    }
                    is Response.Failure -> createFailureResponse(response, requireContext()) {
                        getFilteredMealsByCategory("Chicken")
                        Log.d("HomeFragment", "No meals found for this category")
                    }
                }
            }
        }

       /* // Handle cuisine meals
        with(homeViewModel) {
            chosenCuisine?.let { getFilteredMealsByAreas(it) }
            filteredMealsByAreas.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Response.Loading -> shimmerCuisine.startShimmer()
                    is Response.Success -> {
                        shimmerCuisine.stopShimmer()
                        shimmerCuisine.visibility = View.GONE
                        cuisineAdapter.submitList(response.data.meals)
                    }
                    is Response.Failure -> createFailureResponse(response, requireContext()) {
                        chosenCuisine?.let { getFilteredMealsByAreas(it) }
                    }
                }
            }
        }*/

        // Handle random meals for recommendations
        with(homeViewModel) {
            getRandomMeals(10)
            someRecommendedMeals.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Response.Loading -> shimmerRecommendations.startShimmer()
                    is Response.Success -> {
                        shimmerRecommendations.stopShimmer()
                        shimmerRecommendations.visibility = View.GONE
                        recommendationsAdapter.submitList(response.data)
                    }
                    is Response.Failure -> createFailureResponse(response, requireContext()) {
                        getRandomMeals(10)
                    }
                }
            }
        }

        /*// Handle random meals for gold
        with(homeViewModel)
        {
            getRandomMeals(5, true)
            someGoldMeals.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Response.Loading -> shimmerGold.startShimmer()
                    is Response.Success -> {
                        shimmerGold.stopShimmer()
                        shimmerGold.visibility = View.GONE
                        goldAdapter.submitList(response.data)
                    }
                    is Response.Failure -> createFailureResponse(response, requireContext()) {
                        getRandomMeals(5, true)
                    }
                }
            }
        }*/

        // Handle user cuisines
        with(homeViewModel) {
            filteredMealsByAreas.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Response.Loading -> shimmerCuisine.startShimmer()
                    is Response.Success -> {
                        shimmerCuisine.stopShimmer()
                        shimmerCuisine.visibility = View.GONE
                        Log.d("HomeFragment", "Filtered Meals: ${response.data.meals}")
                        if (response.data.meals.isNotEmpty()) {
                            cuisineAdapter.submitList(response.data.meals)
                        } else {
                            Log.d("HomeFragment", "No meals found for this cuisine")
                        }
                    }
                    is Response.Failure -> {
                        createFailureResponse(response, requireContext()) {
                            // إعادة المحاولة
                        }
                        Log.d("HomeFragment", "Failed to load meals for this cuisine")
                    }
                }
            }
        }


        /*// Handle user cuisines
        with(homeViewModel) {
            getUserCuisines()
            userCuisines.observe(viewLifecycleOwner) { response ->
                when (response) {
                    is Response.Loading -> shimmerCuisine.startShimmer()
                    is Response.Success -> {
                        val data = response.data
                        if (!data.isNullOrEmpty()) {
                            popup.menu.clear()
                            dataViewModel.updateMainCuisine(data[0])
                            dataViewModel.setCuisines(data)
                        } else {

                        }
                    }

                    is Response.Failure -> createFailureResponse(response, requireContext()) {
                        getUserCuisines()
                    }
                }
            }
        }*/
    }

    private fun initializeUi(view: View) {
        recyclerViewCategories = view.findViewById(R.id.recyclerview_categories)
        recyclerViewCategoriesItems= view.findViewById(R.id.recyclerview_mealByCategories)
        recyclerViewRecommendations = view.findViewById(R.id.recyclerview_recommendations)
        recyclerViewCuisine = view.findViewById(R.id.recyclerview_meal_by_fav_cuisine)
//        recyclerViewGold = view.findViewById(R.id.recyclerviewGold)

        shimmerCategories = view.findViewById(R.id.shimmer_categories)
        shimmerCategoriesItem= view.findViewById(R.id.shimmer_mealbycategories)
        shimmerRecommendations = view.findViewById(R.id.shimmer_recommendations)
        shimmerCuisine = view.findViewById(R.id.shimmer_meal_by_fav_cuisine)
//        shimmerGold = view.findViewById(R.id.shimmerGold)

//        constraintLayoutGold = view.findViewById(R.id.constraintlayoutGold)
        cardViewFreeTrial = view.findViewById(R.id.cardViewFreeTrial)

        btnAddToPlan = view.findViewById(R.id.btn_add_to_plan)
        btnDrawer = view.findViewById(R.id.btnHomeDrawer)
        textViewCuisines = view.findViewById(R.id.textViewCuisines)

        categoriesAdapter = AdapterRVCategories(listOf()) { name -> goToSearchCategories(name) }
        categoriesItemAdapter= AdapterRVItemMeal(listOf()) { id -> goToDetails(id) }
        recommendationsAdapter = AdapterRVItemMeal(listOf()) { id -> goToDetails(id) }
        cuisineAdapter = AdapterRVItemMeal(listOf()) { id -> goToDetails(id) }
//        goldAdapter = AdapterRVItemMeal(listOf()) { id -> goToDetails(id) }

        recyclerViewCategories.adapter = categoriesAdapter
        recyclerViewCategoriesItems.adapter= categoriesItemAdapter
        recyclerViewRecommendations.adapter = recommendationsAdapter
        recyclerViewCuisine.adapter = cuisineAdapter
//        recyclerViewGold.adapter = goldAdapter

        setupRecyclerView(recyclerViewCategories)
        setupRecyclerView(recyclerViewCategoriesItems)
        setupRecyclerView(recyclerViewRecommendations)
        setupRecyclerView(recyclerViewCuisine)
//        setupRecyclerView(recyclerViewGold)

        btnCuisines = view.findViewById(R.id.btnCuisines)
        popup = PopupMenu(requireContext(), btnCuisines)
        setupCuisinesPopup()


       /* popup = PopupMenu(requireContext(), btnCuisines).apply {
            setOnMenuItemClickListener { item ->
                homeFavCuisines(item.title.toString())
                true
            }
        }*/

        drawer = requireActivity().findViewById(R.id.drawer)
        searchBarHome = view.findViewById(R.id.searchBar_home)

        btnDrawer.setOnClickListener {
            drawer.openDrawer(GravityCompat.START)
        }

        navController =
            requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                ?.findNavController()
    }

    private fun setupRecyclerView(recyclerView: RecyclerView) {
        recyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
    }

    /*private fun homeFavCuisines(cuisine: String) {
        chosenCuisine = cuisine
        homeViewModel.getFilteredMealsByAreas(cuisine)
        textViewCuisines.text = "$cuisine Recipes for you"
    }
    private fun homeFavCategories(cuisine: String) {
        chosenCategories = cuisine
        homeViewModel.getFilteredMealsByCategory(cuisine)
    }
*/
    private fun goToDetails(id: String) {
        dataViewModel.setItemDetails(id)
        navController?.navigate(R.id.detailsFragment, null, navOptions)
    }

    private fun goToSearchCategories(name: String) {
        dataViewModel.updateSearchCategory(name)
        homeViewModel.getFilteredMealsByCategory(name)
    }

    private fun checkConnection() {
        if (!SystemChecks.isNetworkAvailable(requireContext())) {
            createMaterialAlertDialogBuilderOk(
                requireContext(),
                "No Internet Connection",
                "Please check your internet connection and try again",
                "Retry",
            ) {
                checkConnection()
            }
        } else {
            initObservers()
        }
    }

    private fun loadDefaultMeals() {
        homeViewModel.getDefaultMeals(10) // تحميل 10 وجبات افتراضية
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
                        homeViewModel
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
            val selectedCuisine = item.title.toString()
            homeViewModel.getFilteredMealsByAreas(selectedCuisine)
            textViewCuisines.text = "$selectedCuisine Recipes for you"
            true
        }
    }
    private fun updateFreeTrialCard(meal: Meal?) {
        meal?.let {
            // عرض اسم الوجبة
            val textViewMealName = cardViewFreeTrial.findViewById<TextView>(R.id.meal_name)
            textViewMealName.text = it.strMeal


            val imageViewMeal = cardViewFreeTrial.findViewById<ImageView>(R.id.meal_image)
            Glide.with(requireContext())
                .load(it.strMealThumb)
                .into(imageViewMeal)
        }
    }

}