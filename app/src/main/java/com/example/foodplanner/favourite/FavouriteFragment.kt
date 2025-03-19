package com.example.foodplanner.favourite.view

import FavouriteRecyclerAdapter
import android.icu.lang.UCharacter.GraphemeClusterBreak.L
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
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodplanner.R
import com.example.foodplanner.core.model.local.repository.UserRepositoryImpl
import com.example.foodplanner.core.model.local.source.LocalDataSourceImpl
import com.example.foodplanner.core.model.local.source.UserDatabase
import com.example.foodplanner.core.model.remote.repository.MealRepositoryImpl
import com.example.foodplanner.core.model.remote.source.RemoteGsonDataImpl
import com.example.foodplanner.core.viewmodel.DataViewModel
import com.example.foodplanner.core.viewmodel.DataViewModelFactory
import com.example.foodplanner.utils.NetworkUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
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

    private val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_in_right)
        .setPopExitAnim(R.anim.slide_out_right)
        .build()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favourite, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initObservers()
        favouriteRecycle = view.findViewById(R.id.favouriteRecycle)

        adapter = FavouriteRecyclerAdapter({ id, isChange, onComplete ->
            changeFavouriteState(id, isChange, onComplete)
        }, { id -> goToDetails(id) })

        favouriteRecycle.layoutManager = GridLayoutManager(requireContext(), 2)
        favouriteRecycle.adapter = adapter

        navController =
            requireActivity().supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                ?.findNavController()
    }

    private fun initObservers() {
        dataViewModel.meals.observe(viewLifecycleOwner, Observer { recipes ->
            adapter.updateData(recipes)
        })

        dataViewModel.isFavourite.observe(viewLifecycleOwner, Observer { isFavourite ->
            favouriteState = isFavourite
        })
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

    private fun goToDetails(id: String) {
        Log.d(TAG, "goToDetails called with id: $id")
        if (NetworkUtils.isInternetAvailable(requireContext())) {
            dataViewModel.setItemDetails(id)
            navController?.navigate(R.id.action_details, null, navOptions)
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

}
