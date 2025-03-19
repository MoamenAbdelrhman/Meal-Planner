package com.example.foodplanner.search.view

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.foodplanner.R
import com.example.foodplanner.core.model.remote.repository.MealRepositoryImpl
import com.example.foodplanner.core.model.remote.source.RemoteGsonDataImpl
import com.example.foodplanner.search.adapter.SuggestionAdapter
import com.example.foodplanner.search.viewmodel.SearchViewModel
import com.example.foodplanner.search.viewmodel.SearchViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SuggestionsBottomSheet : BottomSheetDialogFragment() {

    private val searchViewModel: SearchViewModel by viewModels({
        requireParentFragment()
    }) {
        val mealRepository = MealRepositoryImpl(RemoteGsonDataImpl())
        SearchViewModelFactory(mealRepository)
    }

    private lateinit var rvSuggestions: RecyclerView
    private lateinit var etSearchSuggestions: EditText
    private lateinit var suggestionAdapter: SuggestionAdapter
    private var searchType: SearchViewModel.SearchType = SearchViewModel.SearchType.COUNTRY
    private var onSuggestionSelected: ((String) -> Unit)? = null

    companion object {
        const val TAG = "SuggestionsBottomSheet"
        fun newInstance(searchType: SearchViewModel.SearchType, onSuggestionSelected: (String) -> Unit): SuggestionsBottomSheet {
            return SuggestionsBottomSheet().apply {
                this.searchType = searchType
                this.onSuggestionSelected = onSuggestionSelected
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_suggestions_bottom_sheet, container, false)
        rvSuggestions = view.findViewById(R.id.rvSuggestions)
        etSearchSuggestions = view.findViewById(R.id.etSearchSuggestions)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        suggestionAdapter = SuggestionAdapter { suggestion ->
            onSuggestionSelected?.invoke(suggestion)
            dismiss() // إغلاق الـ BottomSheet بعد الاختيار
        }
        rvSuggestions.layoutManager = LinearLayoutManager(requireContext())
        rvSuggestions.adapter = suggestionAdapter

        // مراقبة تغييرات نص البحث
        etSearchSuggestions.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                searchViewModel.filterSuggestions(query, searchType)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        searchViewModel.suggestions.observe(viewLifecycleOwner) { suggestions ->
            suggestionAdapter.submitList(suggestions)
        }

        // تحديث الاقتراحات بناءً على نوع البحث
        searchViewModel.updateSuggestions(searchType)
    }
}