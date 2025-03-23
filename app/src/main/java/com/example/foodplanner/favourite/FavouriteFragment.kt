package com.example.foodplanner.favourite.view

import FavouriteRecyclerAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodplanner.R
import com.example.foodplanner.core.model.local.repository.UserRepositoryImpl
import com.example.foodplanner.core.model.local.source.LocalDataSourceImpl
import com.example.foodplanner.core.model.local.source.UserDatabase
import com.example.foodplanner.core.model.remote.repository.MealRepositoryImpl
import com.example.foodplanner.core.model.remote.source.RemoteGsonDataImpl
import com.example.foodplanner.core.util.CreateMaterialAlertDialogBuilder
import com.example.foodplanner.core.viewmodel.DataViewModel
import com.example.foodplanner.core.viewmodel.DataViewModelFactory
import com.example.foodplanner.utils.NetworkUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class FavouriteFragment : Fragment() {

    private val dataViewModel: DataViewModel by activityViewModels {
        val userRepository = UserRepositoryImpl(
            LocalDataSourceImpl(UserDatabase.getDatabaseInstance(requireContext()).userDao()),
            FirebaseAuth.getInstance()
        )
        val mealRepository = MealRepositoryImpl(RemoteGsonDataImpl())
        val favouriteMealDao = UserDatabase.getDatabaseInstance(requireContext()).favouriteMealDao()
        DataViewModelFactory(userRepository, mealRepository, favouriteMealDao)
    }

    private val TAG = "FavouriteFragment"

    private lateinit var favouriteRecycle: RecyclerView
    private lateinit var adapter: FavouriteRecyclerAdapter
    private var favouriteState = false
    private var navController: NavController? = null
    private var isGuest: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isGuest = requireActivity().intent.getBooleanExtra("IS_GUEST", false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favourite, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        favouriteRecycle = view.findViewById(R.id.favouriteRecycle)
        adapter = FavouriteRecyclerAdapter(
            { id, isChange, onComplete -> changeFavouriteState(id, isChange, onComplete) },
            { id -> goToDetails(id) }
        )

        favouriteRecycle.layoutManager = GridLayoutManager(requireContext(), 2)
        favouriteRecycle.adapter = adapter

        navController = requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            ?.findNavController()

        dataViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading == false) {
                initObservers()
            }
        }
    }

    // Set up observers for favorite meals and their state
    private fun initObservers() {
        dataViewModel.meals.observe(viewLifecycleOwner, Observer { recipes ->
            if (recipes != adapter.getCurrentMeals()) {
                adapter.updateData(recipes)
                Log.d(TAG, "Meals updated: ${recipes.size} items")
            } else {
                Log.d(TAG, "Meals unchanged, skipping update")
            }
        })

        dataViewModel.isFavourite.observe(viewLifecycleOwner, Observer { isFavourite ->
            favouriteState = isFavourite
        })
    }

    // Change the favorite state of a meal with guest restrictions
    private fun changeFavouriteState(
        recipeId: String,
        isChange: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        if (isGuest && isChange) {
            CreateMaterialAlertDialogBuilder.createGuestLoginDialog(requireContext())
            onComplete(false)
            return
        }

        if (isChange && favouriteState) {
            CreateMaterialAlertDialogBuilder.createConfirmRemovalDialog(
                context = requireContext(),
                message = "Are you sure you want to remove this meal from your favorites?",
                positiveAction = {
                    dataViewModel.viewModelScope.launch {
                        dataViewModel.changeFavouriteState(recipeId, true)
                    }.invokeOnCompletion {
                        onComplete(favouriteState)
                    }
                },
                negativeAction = {
                    onComplete(favouriteState)
                }
            )
        } else {
            dataViewModel.viewModelScope.launch {
                dataViewModel.changeFavouriteState(recipeId, isChange)
            }.invokeOnCompletion {
                onComplete(favouriteState)
            }
        }
    }

    // Navigate to meal details screen with the given meal ID
    private fun goToDetails(id: String) {
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            dataViewModel.setItemDetails(id)
            val action = FavouriteFragmentDirections.actionFavouriteToDetails(id)
            findNavController().navigate(action)
        } else {
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

    // Stop syncing favorite meals when the view is destroyed
    override fun onDestroyView() {
        super.onDestroyView()
        if (!isGuest) {
            dataViewModel.stopFavoritesSync()
        }
    }

    // Get the current list of meals from the adapter using reflection
    private fun FavouriteRecyclerAdapter.getCurrentMeals(): List<Any> {
        return try {
            this.javaClass.getDeclaredField("meals").let { field ->
                field.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                field.get(this) as List<Any>
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}