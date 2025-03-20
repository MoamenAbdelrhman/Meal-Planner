package com.example.foodplanner.home.view.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodplanner.R
import com.example.foodplanner.core.model.remote.Category
import com.google.android.material.card.MaterialCardView

class AdapterRVCategories(
    private var categories: List<Category>,
    private var selectedCategory: String? = null,
    private val goToSearch: ((id: String) -> Unit)? = null
) : RecyclerView.Adapter<AdapterRVCategories.CategoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rv_home_categories, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]

        with(holder) {
            textView.text = category.strCategory
            Glide.with(itemView.context)
                .load(category.strCategoryThumb)
                .centerCrop()
                .into(imageView)

            if (category.strCategory == selectedCategory) {
                textView.setTextColor(ContextCompat.getColor(itemView.context, R.color.mycolor))
                cardItemCategory.setCardBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.black))
                cardItemCategory.alpha = 0.5f
            } else {
                textView.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                cardItemCategory.setCardBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.black))
                cardItemCategory.alpha = 0.3f
            }

            itemView.setOnClickListener {
                goToSearch?.let { it(category.strCategory) }
            }
        }
    }

    override fun getItemCount(): Int = categories.size

    fun submitList(newCategories: List<Category>, newSelectedCategory: String? = null) {
        categories = newCategories
        selectedCategory = newSelectedCategory
        notifyDataSetChanged()
    }

    fun resetSelection() {
    }

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.item_category_image)
        val textView: TextView = itemView.findViewById(R.id.item_category_title)
        val cardItemCategory: MaterialCardView = itemView.findViewById(R.id.card_item_category)
    }
}