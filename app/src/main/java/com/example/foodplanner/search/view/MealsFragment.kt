package com.example.foodplanner.search.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import com.google.firebase.auth.FirebaseAuth

class MealsFragment : Fragment() {

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
        MealPlanViewModelFactory( userRepository, requireContext())
    }

    private lateinit var progressBar: ProgressBar
    private lateinit var rvMeals: RecyclerView
    private lateinit var mealsAdapter: MealsAdapter
    private var navController: NavController? = null

    private val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_in_right)
        .setPopExitAnim(R.anim.slide_out_right)
        .build()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_meals, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvMeals = view.findViewById(R.id.rvMeals)
        progressBar = view.findViewById(R.id.progressBar)

        mealsAdapter = MealsAdapter(
            { mealId -> goToDetails(mealId) },
            this,
            mealPlanViewModel
        )
        rvMeals.layoutManager = LinearLayoutManager(requireContext())
        rvMeals.adapter = mealsAdapter

        navController = findNavController()

        val args = arguments ?: return
        val query = args.getString("query") ?: ""
        val searchType = when (args.getString("type")) {
            "COUNTRY" -> SearchViewModel.SearchType.COUNTRY
            "INGREDIENT" -> SearchViewModel.SearchType.INGREDIENT
            "CATEGORY" -> SearchViewModel.SearchType.CATEGORY
            else -> SearchViewModel.SearchType.COUNTRY
        }

        searchViewModel.searchResults.observe(viewLifecycleOwner) { meals ->
            if (meals != null && meals.isNotEmpty()) {
                mealsAdapter.submitList(meals)
                progressBar.visibility = View.GONE
                rvMeals.visibility = View.VISIBLE
            } else {
                progressBar.visibility = View.GONE
                rvMeals.visibility = View.GONE
            }
        }

        if (query.isNotEmpty()) {
            progressBar.visibility = View.VISIBLE
            rvMeals.visibility = View.GONE
            searchViewModel.searchMealsByName(query, searchType)
        } else {
            progressBar.visibility = View.GONE
            rvMeals.visibility = View.GONE

        }
    }


    private fun goToDetails(id: String) {
        dataViewModel.setItemDetails(id)
        navController?.navigate(R.id.action_details, null, navOptions)
    }
}

    /*private fun showAddToPlanDialog(meal: Meal) {
        val days = arrayOf("Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
        AlertDialog.Builder(requireContext())
            .setTitle("Add ${meal.strMeal} to Plan")
            .setItems(days) { _, which ->
                val selectedDay = days[which]
                mealPlanViewModel.addMealToPlan(selectedDay, meal)
                Toast.makeText(requireContext(), "${meal.strMeal} added to $selectedDay", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }*/
