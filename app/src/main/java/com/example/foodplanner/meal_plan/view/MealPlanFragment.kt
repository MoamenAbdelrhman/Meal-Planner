package com.example.foodplanner.meal_plan.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodplanner.R
import com.example.foodplanner.core.model.local.repository.UserRepositoryImpl
import com.example.foodplanner.core.model.local.source.LocalDataSourceImpl
import com.example.foodplanner.core.model.local.source.UserDatabase
import com.example.foodplanner.core.model.remote.repository.MealRepositoryImpl
import com.example.foodplanner.core.model.remote.source.RemoteGsonDataImpl
import com.example.foodplanner.meal_plan.viewModel.MealPlanViewModel
import com.example.foodplanner.meal_plan.viewModel.MealPlanViewModelFactory
import com.example.foodplanner.meal_plan.view.adapter.MealPlanAdapter
import com.google.firebase.auth.FirebaseAuth
import androidx.navigation.fragment.findNavController
import com.example.foodplanner.core.viewmodel.DataViewModel
import com.example.foodplanner.core.viewmodel.DataViewModelFactory
import com.example.foodplanner.utils.NetworkUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MealPlanFragment : Fragment() {

    // ViewModel for managing meal plan data
    private val mealPlanViewModel: MealPlanViewModel by activityViewModels {
        val userRepository = UserRepositoryImpl(
            LocalDataSourceImpl(UserDatabase.getDatabaseInstance(requireContext()).userDao()),
            FirebaseAuth.getInstance()
        )
        MealPlanViewModelFactory(userRepository, requireContext())
    }

    // ViewModel for handling navigation to meal details
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
    private var navController: NavController? = null

    // Navigation options for smooth transitions
    private val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_in_right)
        .setPopExitAnim(R.anim.slide_out_right)
        .build()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_meal_plan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeUi(view)

        // Observe loading state to delay UI updates until data is ready
        mealPlanViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading == false) {
                initObservers()
            }
        }

        // Initialize the navigation controller
        navController = requireActivity().supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment)?.findNavController()
    }

    // Set up the RecyclerView and adapter
    private fun initializeUi(view: View) {
        rvMealPlan = view.findViewById(R.id.rvMealPlan)
        mealPlanAdapter = MealPlanAdapter(
            listOf(),
            mealPlanViewModel,
            goToDetails = { id -> goToDetails(id) }
        )
        rvMealPlan.layoutManager = LinearLayoutManager(requireContext())
        rvMealPlan.adapter = mealPlanAdapter
    }

    // Set up observers for LiveData to update the UI
    private fun initObservers() {
        mealPlanViewModel.weeklyMealPlan.observe(viewLifecycleOwner) { mealPlan ->
            mealPlan?.let {
                Log.d(TAG, "Meal plan updated: $it")
                mealPlanAdapter.submitList(it) // Update the adapter with new data
            } ?: run {
                Log.d(TAG, "Meal plan is null")
            }
        }
    }

    // Navigate to meal details if internet is available
    private fun goToDetails(id: String) {
        Log.d(TAG, "goToDetails called with id: $id")
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            Log.d(TAG, "Internet available, navigating to details")
            dataViewModel.setItemDetails(id)
            navController?.navigate(R.id.action_details, null, navOptions)
        } else {
            Log.d(TAG, "No internet connection, showing dialog")
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("No Internet Connection")
                .setMessage("Please check your internet connection and try again.")
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(true)
                .show()
        }
    }
}