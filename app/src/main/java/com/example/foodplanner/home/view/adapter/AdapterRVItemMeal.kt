package com.example.foodplanner.home.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodplanner.R
import com.example.foodplanner.core.model.remote.Meal

class AdapterRVItemMeal(
    private var meals: List<Meal>,
    private val goToDetails: ((id: String) -> Unit)? = null
) : RecyclerView.Adapter<AdapterRVItemMeal.MealViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rv_main_view_of_meals, parent, false)
        return MealViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val meal = meals[position]

        holder.titleView.text = formatDescription(meal.strMeal)

        Glide.with(holder.itemView.context)
            .load(meal.strMealThumb)
            .centerCrop()
            .into(holder.imageView)

        holder.imageView.setOnClickListener {
            goToDetails?.let { it(meal.idMeal) }
        }
    }

    override fun getItemCount(): Int = meals.size

    fun submitList(newMeals: List<Meal>) {
        meals = newMeals
        notifyDataSetChanged()
    }

    private fun formatDescription(
        description: String,
        maxLength: Int = 50,
        suffix: String = " ...Show more"
    ): String {
        return if (description.length > maxLength) {
            "${description.substring(0, maxLength)}$suffix"
        } else {
            description
        }
    }

    class MealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.item_image)
        val titleView: TextView = itemView.findViewById(R.id.item_title)
    }
}
