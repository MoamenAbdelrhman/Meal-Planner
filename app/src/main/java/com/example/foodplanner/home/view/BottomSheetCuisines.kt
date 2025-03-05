package com.example.foodplanner.home.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodplanner.R
import com.example.foodplanner.core.model.local.repository.UserRepositoryImpl
import com.example.foodplanner.core.model.local.source.LocalDataSourceImpl
import com.example.foodplanner.core.model.local.source.UserDatabase
import com.example.foodplanner.core.model.remote.Response
import com.example.foodplanner.core.model.remote.repository.MealRepositoryImpl
import com.example.foodplanner.core.model.remote.source.RemoteGsonDataImpl
import com.example.foodplanner.core.util.CreateMaterialAlertDialogBuilder.createFailureResponse
import com.example.foodplanner.core.viewmodel.DataViewModel
import com.example.foodplanner.core.viewmodel.DataViewModelFactory
import com.example.foodplanner.home.view.adapter.AdapterRVCuisines
import com.example.foodplanner.home.viewModel.BottomSheetCuisinesViewModel
import com.example.foodplanner.home.viewModel.BottomSheetCuisinesViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth

class BottomSheetCuisines : BottomSheetDialogFragment() {

    private val viewModel: BottomSheetCuisinesViewModel by viewModels {
        val remoteGsonDataSource = RemoteGsonDataImpl()
        val mealRepository = MealRepositoryImpl(remoteGsonDataSource)
        val userRepository = UserRepositoryImpl(
            LocalDataSourceImpl(UserDatabase.getDatabaseInstance(requireContext()).userDao()),
            FirebaseAuth.getInstance()
        )
        BottomSheetCuisinesViewModelFactory(userRepository, mealRepository)
    }

    private val dataViewModel: DataViewModel by activityViewModels {
        val userRepository = UserRepositoryImpl(
            LocalDataSourceImpl(UserDatabase.getDatabaseInstance(requireContext()).userDao()),
            FirebaseAuth.getInstance()
        )
        val mealRepository = MealRepositoryImpl(RemoteGsonDataImpl())
        DataViewModelFactory(userRepository, mealRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_cuisines, container, false)
        val btnDone: Button = view.findViewById(R.id.buttonDone)
        val recyclerviewCuisines = view.findViewById<RecyclerView>(R.id.recyclerviewCuisines)
        val gridLayoutManager = GridLayoutManager(requireContext(), 3)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBarBottomSheetCuisines)

        recyclerviewCuisines.layoutManager = gridLayoutManager

        dataViewModel.cuisinesData.observe(viewLifecycleOwner) { dismiss() }

        viewModel.getAllCuisines()

        viewModel.allCuisines.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Response.Loading -> {
                    progressBar.visibility = View.VISIBLE
                }

                is Response.Success -> {
                    val adapter = AdapterRVCuisines(response.data.meals) { lastCuisine ->
                        dataViewModel.updateMainCuisine(lastCuisine)
                    }
                    progressBar.visibility = View.GONE
                    recyclerviewCuisines.adapter = adapter
                    btnDone.setOnClickListener { dataViewModel.setCuisines(adapter.getSelectedCuisines()) }
                }

                is Response.Failure -> {
                    createFailureResponse(response, requireContext())
                }
            }
        }
        return view
    }

    companion object {
        const val TAG = "BottomSheetCuisines"
    }
}