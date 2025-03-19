package com.example.foodplanner.search.model

import com.example.foodplanner.search.viewmodel.SearchViewModel

data class SuggestionItem(
    val name: String,
    val imageUrl: String? = null,
    val type: SearchViewModel.SearchType
)