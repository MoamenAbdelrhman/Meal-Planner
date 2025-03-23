package com.example.foodplanner.meal_plan.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
import com.example.foodplanner.meal_plan.view.adapter.MealPlanAdapter
import com.example.foodplanner.meal_plan.viewModel.MealPlanViewModel
import com.example.foodplanner.meal_plan.viewModel.MealPlanViewModelFactory
import com.example.foodplanner.utils.NetworkUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MealPlanFragment : Fragment() {

    private val mealPlanViewModel: MealPlanViewModel by activityViewModels {
        val userRepository = UserRepositoryImpl(
            LocalDataSourceImpl(UserDatabase.getDatabaseInstance(requireContext()).userDao()),
            FirebaseAuth.getInstance()
        )
        MealPlanViewModelFactory(userRepository, requireContext())
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

    private val TAG = "MealPlanFragment"

    private lateinit var rvMealPlan: RecyclerView
    private lateinit var mealPlanAdapter: MealPlanAdapter
    private var isGuest: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isGuest = requireActivity().intent.getBooleanExtra("IS_GUEST", false)
        return inflater.inflate(R.layout.fragment_meal_plan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isGuest) {
            Toast.makeText(requireContext(), "Guests cannot access Meal Plan. Please log in.", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_meal_plan_to_home)
            return
        }

        initializeUi(view)

        mealPlanViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading == false) {
                initObservers()
            }
        }
    }

    private fun initializeUi(view: View) {
        rvMealPlan = view.findViewById(R.id.rvMealPlan)
        mealPlanAdapter = MealPlanAdapter(
            listOf(),
            mealPlanViewModel,
            goToDetails = { id -> goToDetails(id) },
            isGuest = isGuest,
            onAddMeal = { dayName, mealId ->
                lifecycleScope.launch {
                    val meal = dataViewModel.getMealById(mealId)
                    meal?.let {
                        mealPlanViewModel.addMealToPlan(dayName, it)
                    } ?: run {
                        Toast.makeText(requireContext(), "Meal not found", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        rvMealPlan.layoutManager = LinearLayoutManager(requireContext())
        rvMealPlan.adapter = mealPlanAdapter
    }

    private fun initObservers() {
        mealPlanViewModel.weeklyMealPlan.observe(viewLifecycleOwner) { mealPlan ->
            mealPlan?.let {
                Log.d(TAG, "Meal plan updated: $it")
                mealPlanAdapter.submitList(it)
            } ?: run {
                Log.d(TAG, "Meal plan is null")
            }
        }
    }

    private fun goToDetails(id: String) {
        Log.d(TAG, "goToDetails called with id: $id")
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            dataViewModel.setItemDetails(id)
            val action = MealPlanFragmentDirections.actionMealPlanToDetails(id)
            findNavController().navigate(action)
        } else {
            Log.d(TAG, "No internet connection, showing dialog")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("No Internet Connection")
                .setMessage("Please check your internet connection and try again.")
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .setCancelable(true)
                .show()
        }
    }
}