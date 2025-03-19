package com.example.foodplanner.meal_plan.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodplanner.R
import com.example.foodplanner.core.model.remote.Meal

class DayMealAdapter(
    private var meals: List<Meal>,
    private val onDeleteClick: (Meal) -> Unit,
    private val goToDetails: (String) -> Unit
) : RecyclerView.Adapter<DayMealAdapter.MealViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day_meal_inner, parent, false)
        return MealViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val meal = meals[position]
        holder.mealNameTextView.text = meal.strMeal

        Glide.with(holder.itemView.context)
            .load(meal.strMealThumb)
            .centerCrop()
            .into(holder.mealImageView)

        holder.mealImageView.setOnClickListener {
            goToDetails(meal.idMeal)
        }

        holder.mealNameTextView.setOnClickListener {
            goToDetails(meal.idMeal)
        }

        holder.btnDeleteMeal.setOnClickListener {
            onDeleteClick(meal)
        }
    }

    override fun getItemCount(): Int = meals.size

    fun submitList(newMeals: List<Meal>) {
        meals = newMeals.toList()
        notifyDataSetChanged()
    }

    class MealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mealImageView: ImageView = itemView.findViewById(R.id.ivMealImage)
        val mealNameTextView: TextView = itemView.findViewById(R.id.tvMealName)
        val btnDeleteMeal: ImageButton = itemView.findViewById(R.id.btnDeleteMeal)
    }
}