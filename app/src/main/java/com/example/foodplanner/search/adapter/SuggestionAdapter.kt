package com.example.foodplanner.search.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodplanner.R
import com.example.foodplanner.search.model.SuggestionItem
import com.example.foodplanner.search.viewmodel.SearchViewModel

class SuggestionAdapter(
    private val onItemClick: (String) -> Unit
) : ListAdapter<SuggestionItem, SuggestionAdapter.SuggestionViewHolder>(SuggestionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_suggestion, parent, false)
        return SuggestionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        val suggestion = getItem(position)
        holder.bind(suggestion)
        holder.itemView.setOnClickListener { onItemClick(suggestion.name) }
    }

    class SuggestionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSuggestion: TextView = itemView.findViewById(R.id.tvSuggestion)
        private val ivSuggestionImage: ImageView = itemView.findViewById(R.id.ivSuggestionImage)

        fun bind(suggestion: SuggestionItem) {
            tvSuggestion.text = suggestion.name
            println("Loading image for ${suggestion.name}: ${suggestion.imageUrl}")

            if (suggestion.imageUrl != null) {
                if (suggestion.type == SearchViewModel.SearchType.CATEGORY || suggestion.type == SearchViewModel.SearchType.INGREDIENT) {
                    ivSuggestionImage.layoutParams.width = 140
                    ivSuggestionImage.layoutParams.height = 140
                } else {
                    ivSuggestionImage.layoutParams.width = 120
                    ivSuggestionImage.layoutParams.height = 120
                }

                Glide.with(itemView.context)
                    .load(suggestion.imageUrl)
                    .placeholder(R.drawable.error_ingredient)
                    .error(R.drawable.error_ingredient)
                    .into(ivSuggestionImage)
                ivSuggestionImage.visibility = View.VISIBLE
            } else {
                ivSuggestionImage.visibility = View.GONE
            }
        }
    }

    class SuggestionDiffCallback : DiffUtil.ItemCallback<SuggestionItem>() {
        override fun areItemsTheSame(oldItem: SuggestionItem, newItem: SuggestionItem): Boolean {
            return oldItem.name == newItem.name && oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: SuggestionItem, newItem: SuggestionItem): Boolean {
            return oldItem == newItem
        }
    }
}