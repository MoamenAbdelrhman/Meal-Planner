package com.example.foodplanner.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.foodplanner.Domain.Country
import com.example.foodplanner.R

class CountryAdapter(
    context: Context,
    private val countries: List<Country>
) : ArrayAdapter<Country>(context, 0, countries) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_country_spinner, parent, false)

        val country = getItem(position)
        val imageView = view.findViewById<ImageView>(R.id.ivFlag)
        val textView = view.findViewById<TextView>(R.id.tvCountryName)

        country?.let {
            imageView.setImageResource(it.flagResId)
            textView.text = it.name
        }
        return view
    }
}
