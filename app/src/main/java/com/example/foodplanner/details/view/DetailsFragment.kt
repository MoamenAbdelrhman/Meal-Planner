package com.example.foodplanner.details.view

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.foodplanner.R
import com.example.foodplanner.core.model.local.repository.UserRepositoryImpl
import com.example.foodplanner.core.model.local.source.LocalDataSourceImpl
import com.example.foodplanner.core.model.local.source.UserDatabase
import com.example.foodplanner.core.model.remote.Meal
import com.example.foodplanner.core.model.remote.repository.MealRepositoryImpl
import com.example.foodplanner.core.model.remote.source.RemoteGsonDataImpl
import com.example.foodplanner.core.viewmodel.DataViewModel
import com.example.foodplanner.core.viewmodel.DataViewModelFactory
import com.example.foodplanner.details.view.adapter.DetailsAdapter
import com.example.foodplanner.details.viewmodel.DetailsFactory
import com.example.foodplanner.details.viewmodel.DetailsViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.launch

class DetailsFragment : Fragment() {


    private val detailsViewModel: DetailsViewModel by viewModels {
        val mealRepository = MealRepositoryImpl(RemoteGsonDataImpl())
        DetailsFactory(mealRepository)
    }

    private val dataViewModel: DataViewModel by activityViewModels {
        val userRepository = UserRepositoryImpl(
            LocalDataSourceImpl(UserDatabase.getDatabaseInstance(requireContext()).userDao()),
            FirebaseAuth.getInstance()
        )
        val mealRepository = MealRepositoryImpl(RemoteGsonDataImpl())
        DataViewModelFactory(userRepository, mealRepository)
    }

    // Youtube
    private lateinit var youtubePlayerView: YouTubePlayerView
    private lateinit var youtubePlayer: YouTubePlayer
    private lateinit var fullScreenContainer: FrameLayout
    private lateinit var iFramePlayerOptions: IFramePlayerOptions
    private var isFullscreen = false
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (isFullscreen) {
                // if the player is in fullscreen, exit fullscreen
                youtubePlayer.toggleFullscreen()
            } else {
                navController.popBackStack()
            }
        }
    }

    private var recipeId: String = "52772"
    private lateinit var navController: NavController
    private lateinit var ingredientsRecycler: RecyclerView
    private lateinit var ingredientsAdapter: DetailsAdapter
    private lateinit var ingredientsCard: MaterialCardView
    private lateinit var instructionsCard: MaterialCardView
    private lateinit var mealName: TextView
    private lateinit var mealCategory: TextView
    private lateinit var mealArea: TextView
    private lateinit var instructionsText: TextView
    private lateinit var backImage: ImageView
    private lateinit var mealImage: ImageView
    private lateinit var favouriteImage: ImageView
    private lateinit var ingredientsArrow: ImageView
    private lateinit var instructionsArrow: ImageView
    private lateinit var bottomNavigationView: BottomNavigationView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        initObservers()
        initListeners()
    }

    private fun initViews() {
        // Youtube
        youtubePlayerView = requireView().findViewById(R.id.youtubePlayer)
        fullScreenContainer = requireView().findViewById(R.id.fullScreenContainer)
        iFramePlayerOptions = IFramePlayerOptions.Builder()
            .controls(1)
            .fullscreen(1) // enable full screen button
            .build()
        youtubePlayerView.enableAutomaticInitialization = false
        lifecycle.addObserver(youtubePlayerView)

        bottomNavigationView = requireActivity().findViewById(R.id.bottom_navigation)
        navController = findNavController()
        ingredientsRecycler = requireView().findViewById(R.id.ingredientsRecycler)
        ingredientsAdapter = DetailsAdapter()
        ingredientsCard = requireView().findViewById(R.id.ingredientsCard)
        instructionsCard = requireView().findViewById(R.id.instructionsCard)
        backImage = requireView().findViewById(R.id.btn_back)
        mealImage = requireView().findViewById(R.id.mealImage)
        favouriteImage = requireView().findViewById(R.id.btn_favorite)
        ingredientsArrow = requireView().findViewById(R.id.ingredientsArrow)
        instructionsArrow = requireView().findViewById(R.id.instructionsArrow)
        mealName = requireView().findViewById(R.id.mealTitle)
        mealCategory = requireView().findViewById(R.id.category)
        mealArea = requireView().findViewById(R.id.mealArea)
        instructionsText = requireView().findViewById(R.id.instructionsText)

        bottomNavigationView.visibility = View.GONE
    }


    private fun initListeners() {
        backImage.setOnClickListener { navController.popBackStack() }

        favouriteImage.setOnClickListener {
            changeFavouriteState(recipeId, true)
        }

        ingredientsCard.setOnClickListener {
            ingredientsRecycler.isVisible = !ingredientsRecycler.isVisible
            ingredientsArrow.rotation = if (ingredientsRecycler.isVisible) 180f else 0f
        }

        instructionsCard.setOnClickListener {
            instructionsText.isVisible = !instructionsText.isVisible
            instructionsArrow.rotation = if (instructionsText.isVisible) 180f else 0f
        }
    }

    private fun initObservers() {
        dataViewModel.itemDetails.observe(viewLifecycleOwner) { id ->
            recipeId = id
            changeFavouriteState(recipeId, false)
            detailsViewModel.getMealDetails(id)
        }

        detailsViewModel.recipe.observe(viewLifecycleOwner) { result ->
            bindData(result)
        }

        dataViewModel.isFavourite.observe(viewLifecycleOwner) { result ->
            favouriteImage.setImageResource(
                when (result) {
                    true -> R.drawable.loved_icon
                    false -> R.drawable.heart
                }
            )
        }
    }

    private fun bindData(recipe: Meal) {
        ingredientsAdapter.updateData(recipe.listIngredientsWithMeasures)
        ingredientsRecycler.adapter = ingredientsAdapter
        ingredientsRecycler.layoutManager = LinearLayoutManager(requireContext())
        mealName.text = recipe.strMeal
        mealCategory.text = recipe.strCategory
        mealArea.text = recipe.strArea
        Glide.with(requireContext()).load(recipe.strMealThumb).into(mealImage)
        instructionsText.text = recipe.strInstructions
        activateYoutubePlayer(recipe.strYoutube)
    }

    private fun changeFavouriteState(recipeId: String, isChange: Boolean) {
        dataViewModel.viewModelScope.launch {
            dataViewModel.changeFavouriteState(recipeId, isChange)
        }
    }

    private fun activateYoutubePlayer(url: String) {
        val youtubePlayerListener = object : AbstractYouTubePlayerListener() {
            override fun onReady(youTubePlayer: YouTubePlayer) {
                val videoId = detailsViewModel.extractYouTubeVideoId(url)
                youtubePlayer = youTubePlayer
                youtubePlayer.cueVideo(videoId, 0f)
            }
        }

        youtubePlayerView.addFullscreenListener(object : FullscreenListener {
            override fun onEnterFullscreen(fullscreenView: View, exitFullscreen: () -> Unit) {
                isFullscreen = true
                fullScreenContainer.addView(fullscreenView)
                fullScreenContainer.visibility = View.VISIBLE
                WindowInsetsControllerCompat(
                    requireActivity().window, requireView().findViewById(R.id.rootView)
                ).apply {
                    hide(WindowInsetsCompat.Type.statusBars())
                    systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }

                if (requireActivity().requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    requireActivity().requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
            }

            override fun onExitFullscreen() {
                isFullscreen = false
                fullScreenContainer.removeAllViews()
                fullScreenContainer.visibility = View.GONE
                WindowInsetsControllerCompat(
                    requireActivity().window, requireView().findViewById(R.id.rootView)
                ).apply {
                    show(WindowInsetsCompat.Type.statusBars())
                }
                if (requireActivity().requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_SENSOR) {
                    requireActivity().requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                }
            }

        })
        youtubePlayerView.enableAutomaticInitialization = false
        youtubePlayerView.initialize(youtubePlayerListener, iFramePlayerOptions)
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (!isFullscreen) {
                youtubePlayer.toggleFullscreen()
            }
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (isFullscreen) {
                youtubePlayer.toggleFullscreen()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bottomNavigationView.visibility = View.VISIBLE
        youtubePlayerView.release()
    }
}