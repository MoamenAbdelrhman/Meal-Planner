package com.example.foodplanner.details.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodplanner.R

class DetailsAdapter : RecyclerView.Adapter<DetailsAdapter.ViewHolder>() {

    private var data: List<Pair<String, String>> = emptyList()



    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ingredientName: TextView = itemView.findViewById(R.id.ingredientName)
        val ingredientMeasure: TextView = itemView.findViewById(R.id.ingredientMeasure)
        val ingredientImage: ImageView = itemView.findViewById(R.id.ingredientImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater: LayoutInflater = LayoutInflater.from(parent.context)
        val view: View = inflater.inflate(R.layout.simple_list_text, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (name, measure) = data[position]
        holder.ingredientName.text = name
        holder.ingredientMeasure.text = measure

        val imageUrl = getIngredientImageUrl(name)
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .placeholder(R.drawable.error_ingredient)
            .error(R.drawable.error_ingredient)
            .into(holder.ingredientImage)
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun updateData(data: List<Pair<String, String>>) {
        this.data = data
        notifyDataSetChanged()
    }

    private fun getIngredientImageUrl(ingredientName: String): String {
        val formattedName = ingredientName.lowercase().replace(" ", "_")
        return "https://www.themealdb.com/images/ingredients/$formattedName.png"
    }

}