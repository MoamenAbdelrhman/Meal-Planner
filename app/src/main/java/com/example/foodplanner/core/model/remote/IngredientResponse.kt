package com.example.foodplanner.core.model.remote

data class IngredientResponse(
    val meals: List<Ingredient>
)

data class Ingredient(
    val strIngredient: String
)