package com.example.foodplanner.search.adapter

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodplanner.R
import com.example.foodplanner.core.model.remote.Meal
import com.example.foodplanner.meal_plan.viewModel.MealPlanViewModel
import com.example.foodplanner.search.view.MealsFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MealsAdapter(
    private val onItemClick: (String) -> Unit,
    private val fragment: Fragment,
    private val mealPlanViewModel: MealPlanViewModel
) : ListAdapter<Meal, MealsAdapter.MealViewHolder>(MealDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_meal, parent, false)
        return MealViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val meal = getItem(position)
        holder.bind(meal)
        holder.itemView.setOnClickListener { onItemClick(meal.idMeal) }
        holder.ivMealImage.setOnClickListener { onItemClick(meal.idMeal) }
        holder.tvMealName.setOnClickListener { onItemClick(meal.idMeal) }
        holder.btnAddToPlan.setOnClickListener {
            showDaySelectionDialog(meal)
        }
    }

    private fun showDaySelectionDialog(meal: Meal) {
        val days = arrayOf("Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
        AlertDialog.Builder(fragment.requireContext())
            .setTitle("What day would you like to add ${meal.strMeal} ?")
            .setItems(days) { _, which ->
                val selectedDay = days[which]
                mealPlanViewModel.addMealToPlan(selectedDay, meal)
                Toast.makeText(fragment.requireContext(), "${meal.strMeal} added to $selectedDay", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    class MealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivMealImage: ImageView = itemView.findViewById(R.id.ivMealImage)
        val tvMealName: TextView = itemView.findViewById(R.id.tvMealName)
        val btnAddToPlan: Button = itemView.findViewById(R.id.btnAddToPlan)

        fun bind(meal: Meal) {
            tvMealName.text = meal.strMeal
            Glide.with(itemView.context)
                .load(meal.strMealThumb)
                .placeholder(R.drawable.error_ingredient)
                .error(R.drawable.error_ingredient)
                .into(ivMealImage)
        }
    }

    class MealDiffCallback : DiffUtil.ItemCallback<Meal>() {
        override fun areItemsTheSame(oldItem: Meal, newItem: Meal): Boolean {
            return oldItem.idMeal == newItem.idMeal
        }

        override fun areContentsTheSame(oldItem: Meal, newItem: Meal): Boolean {
            return oldItem == newItem
        }
    }
}