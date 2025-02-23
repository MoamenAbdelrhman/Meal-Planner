package com.example.foodplanner.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import com.example.foodplanner.Adapters.CountryAdapter
import com.example.foodplanner.Domain.Country
import com.example.foodplanner.R


class HomeFragment : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

       /* val spinner = view.findViewById<Spinner>(R.id.spinnerCountries)
//        val selectedCountryTextView = view.findViewById<TextView>(R.id.tvSelectedCountry)
//        val selectedFlagImageView = view.findViewById<ImageView>(R.id.ivSelectedFlag)

        val countries = listOf(
            Country("Egypt", R.drawable.flag_egypt),
            Country("Nigeria", R.drawable.flag_nigeria),
            Country("France", R.drawable.flag_france),
            Country("USA", R.drawable.flag_usa),
            Country("Egypt", R.drawable.flag_egypt),
            Country("Nigeria", R.drawable.flag_nigeria),
            Country("France", R.drawable.flag_france),
            Country("USA", R.drawable.flag_usa),
            Country("Egypt", R.drawable.flag_egypt),
            Country("Nigeria", R.drawable.flag_nigeria),
            Country("France", R.drawable.flag_france),
            Country("USA", R.drawable.flag_usa),
        )
        val adapter = CountryAdapter(requireActivity(), countries)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCountry = countries[position]
//                selectedCountryTextView.text = selectedCountry.name
//                selectedFlagImageView.setImageResource(selectedCountry.flagResId)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }*/

    }


}