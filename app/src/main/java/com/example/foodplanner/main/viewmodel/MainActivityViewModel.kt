package com.example.foodplanner.main.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivityViewModel(

) : ViewModel() {

    private val _navigateToFragment = MutableLiveData<Int>()
    val navigateToFragment: LiveData<Int> get() = _navigateToFragment

    fun navigateTo(fragmentName: Int) {
        _navigateToFragment.value = fragmentName
    }
}

class MainActivityViewModelFactory: ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainActivityViewModel::class.java)) {
            return MainActivityViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}