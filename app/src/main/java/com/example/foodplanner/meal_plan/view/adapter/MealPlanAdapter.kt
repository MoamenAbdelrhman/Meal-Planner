package com.example.foodplanner.meal_plan.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodplanner.R
import android.widget.TextView
import com.example.foodplanner.meal_plan.view.DayMealPlan
import com.example.foodplanner.meal_plan.viewModel.MealPlanViewModel
import android.widget.Toast
import com.example.foodplanner.core.util.CreateMaterialAlertDialogBuilder

class MealPlanAdapter(
    private var mealPlan: List<DayMealPlan>,
    private val mealPlanViewModel: MealPlanViewModel,
    private val goToDetails: (String) -> Unit,
    private val isGuest: Boolean = false,
    private val onAddMeal: (String, String) -> Unit
) : RecyclerView.Adapter<MealPlanAdapter.DayViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_day_meal, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val dayMeal = mealPlan[position]
        holder.tvDayName.text = dayMeal.dayName
        holder.dayMealAdapter.submitList(dayMeal.meals)
    }

    override fun getItemCount(): Int = mealPlan.size

    fun submitList(newMealPlan: List<DayMealPlan>) {
        mealPlan = newMealPlan.toList()
        notifyDataSetChanged()
    }

    inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDayName: TextView = itemView.findViewById(R.id.tvDayName)
        val rvDayMeals: RecyclerView = itemView.findViewById(R.id.rvDayMeals)
        val dayMealAdapter: DayMealAdapter

        init {
            dayMealAdapter = DayMealAdapter(
                listOf(),
                onDeleteClick = { meal ->
                    if (isGuest) {
                        Toast.makeText(
                            itemView.context,
                            "Guests cannot modify meal plans. Please log in.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val day = mealPlan[adapterPosition]
                        CreateMaterialAlertDialogBuilder.createConfirmRemovalDialog(
                            context = itemView.context,
                            message = "Are you sure you want to remove this meal from your plan?",
                            positiveAction = {
                                mealPlanViewModel.deleteMealFromPlan(day.dayName, meal)
                            },
                            negativeAction = {
                            }
                        )
                    }
                },
                goToDetails = goToDetails
            )
            rvDayMeals.layoutManager = LinearLayoutManager(
                itemView.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            rvDayMeals.adapter = dayMealAdapter
        }
    }
}