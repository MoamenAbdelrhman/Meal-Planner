package com.example.foodplanner.details.view

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
import com.example.foodplanner.core.util.CreateMaterialAlertDialogBuilder
import com.example.foodplanner.core.viewmodel.DataViewModel
import com.example.foodplanner.core.viewmodel.DataViewModelFactory
import com.example.foodplanner.details.view.adapter.DetailsAdapter
import com.example.foodplanner.details.viewmodel.DetailsFactory
import com.example.foodplanner.details.viewmodel.DetailsViewModel
import com.example.foodplanner.meal_plan.viewModel.MealPlanViewModel
import com.example.foodplanner.meal_plan.viewModel.MealPlanViewModelFactory
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.text.style.ForegroundColorSpan
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        val favouriteMealDao = UserDatabase.getDatabaseInstance(requireContext()).favouriteMealDao()
        DataViewModelFactory(userRepository, mealRepository, favouriteMealDao)
    }

    private val mealPlanViewModel: MealPlanViewModel by activityViewModels {
        val userRepository = UserRepositoryImpl(
            LocalDataSourceImpl(UserDatabase.getDatabaseInstance(requireContext()).userDao()),
            FirebaseAuth.getInstance()
        )
        MealPlanViewModelFactory(userRepository, requireContext())
    }

    // Youtube
    private lateinit var youtubePlayerView: YouTubePlayerView
    private lateinit var youtubePlayer: YouTubePlayer
    private lateinit var fullScreenContainer: FrameLayout
    private lateinit var iFramePlayerOptions: IFramePlayerOptions
    private var isFullscreen = false

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
    private var favouriteImage: ImageView? = null
    private lateinit var ingredientsArrow: ImageView
    private lateinit var instructionsArrow: ImageView
    private lateinit var btnShare: ImageButton
    private lateinit var btnAddToPlanDetails: Button
    private var currentMeal: Meal? = null
    private var isGuest: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isGuest = requireActivity().intent.getBooleanExtra("IS_GUEST", false)
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

    // Initialize UI components and set up YouTube player and RecyclerView
    private fun initViews() {
        youtubePlayerView = requireView().findViewById(R.id.youtubePlayer)
        fullScreenContainer = requireView().findViewById(R.id.fullScreenContainer)
        iFramePlayerOptions = IFramePlayerOptions.Builder()
            .controls(1)
            .fullscreen(1)
            .build()
        youtubePlayerView.enableAutomaticInitialization = false
        lifecycle.addObserver(youtubePlayerView)

        navController = findNavController()
        ingredientsRecycler = requireView().findViewById(R.id.ingredientsRecycler)
        ingredientsAdapter = DetailsAdapter()
        ingredientsCard = requireView().findViewById(R.id.ingredientsCard)
        instructionsCard = requireView().findViewById(R.id.instructionsCard)
        backImage = requireView().findViewById(R.id.profileBackBtn)
        mealImage = requireView().findViewById(R.id.mealImage)
        favouriteImage = requireView().findViewById(R.id.btnDetailFavourite)
        ingredientsArrow = requireView().findViewById(R.id.ingredientsArrow)
        instructionsArrow = requireView().findViewById(R.id.instructionsArrow)
        mealName = requireView().findViewById(R.id.mealTitle)
        mealCategory = requireView().findViewById(R.id.category)
        mealArea = requireView().findViewById(R.id.mealArea)
        instructionsText = requireView().findViewById(R.id.instructionsText)
        btnShare = requireView().findViewById(R.id.btn_share)
        btnAddToPlanDetails = requireView().findViewById(R.id.btnAddToPlanDetails)

        instructionsText.isVisible = false
        instructionsArrow.rotation = 0f

        ingredientsRecycler.layoutManager = LinearLayoutManager(requireContext())
    }

    // Set up click listeners for UI elements like back button, favorite, and share
    private fun initListeners() {
        backImage.setOnClickListener { navController.popBackStack() }

        favouriteImage?.setOnClickListener {
            if (isGuest) {
                showGuestRestrictionDialog("Guests cannot add favorites. Please log in.")
            } else {
                val isCurrentlyFavourite = dataViewModel.isFavourite.value ?: false
                if (isCurrentlyFavourite) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Confirm Removal")
                        .setMessage("Are you sure you want to remove ${currentMeal?.strMeal ?: "this meal"} from your favorites?")
                        .setPositiveButton("Yes") { _, _ ->
                            changeFavouriteState(recipeId, true)
                        }
                        .setNegativeButton("No") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .setCancelable(true)
                        .show()
                } else {
                    changeFavouriteState(recipeId, true)
                }
            }
        }

        ingredientsCard.setOnClickListener {
            ingredientsRecycler.isVisible = !ingredientsRecycler.isVisible
            ingredientsArrow.rotation = if (ingredientsRecycler.isVisible) 180f else 0f
        }

        instructionsCard.setOnClickListener {
            instructionsText.isVisible = !instructionsText.isVisible
            instructionsArrow.rotation = if (instructionsText.isVisible) 180f else 0f
        }

        btnShare.setOnClickListener {
            shareMeal()
        }

        btnAddToPlanDetails.setOnClickListener {
            if (isGuest) {
                showGuestRestrictionDialog("Guests cannot add meals to the plan. Please log in.")
            } else {
                currentMeal?.let { showAddToPlanDialog(it) }
            }
        }
    }

    // Set up observers for meal details and favorite state
    private fun initObservers() {
        dataViewModel.itemDetails.observe(viewLifecycleOwner) { id ->
            recipeId = id
            changeFavouriteState(recipeId, false)
            detailsViewModel.getMealDetails(id)
        }

        detailsViewModel.recipe.observe(viewLifecycleOwner) { result ->
            currentMeal = result
            bindData(result)
        }

        dataViewModel.isFavourite.observe(viewLifecycleOwner) { result ->
            favouriteImage?.setImageResource(
                when (result) {
                    true -> R.drawable.loved_icon
                    false -> R.drawable.icons_favorite48
                }
            )
        }
    }

    // Bind meal data to UI elements and activate YouTube player
    private fun bindData(recipe: Meal) {
        ingredientsAdapter.updateData(recipe.getIngredientsWithMeasurements())
        ingredientsRecycler.adapter = ingredientsAdapter
        mealName.text = recipe.strMeal
        mealCategory.text = recipe.strCategory
        mealArea.text = recipe.strArea
        Glide.with(requireContext()).load(recipe.strMealThumb).into(mealImage)
        instructionsText.text = formatInstructions(recipe.strInstructions)
        activateYoutubePlayer(recipe.strYoutube)
    }

    // Format meal instructions with numbered steps and styling
    private fun formatInstructions(instructions: String): SpannableStringBuilder {
        val steps = instructions.split("\r\n").filter { it.isNotBlank() }
        val formattedText = SpannableStringBuilder()

        steps.forEachIndexed { index, step ->
            val stepLabel = "Step ${index + 1} "
            formattedText.append(stepLabel)
            val labelStart = formattedText.length - stepLabel.length
            val labelEnd = formattedText.length
            formattedText.setSpan(
                StyleSpan(Typeface.BOLD),
                labelStart,
                labelEnd,
                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            formattedText.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(requireContext(), R.color.mycolor)),
                labelStart,
                labelEnd,
                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            formattedText.append(step.trim())
            if (index < steps.size - 1) {
                formattedText.append("\n\n")
            }
        }
        return formattedText
    }

    // Change the favorite state of the meal
    private fun changeFavouriteState(recipeId: String, isChange: Boolean) {
        dataViewModel.viewModelScope.launch {
            dataViewModel.changeFavouriteState(recipeId, isChange)
        }
    }

    // Share the meal details via an intent
    private fun shareMeal() {
        currentMeal?.let { meal ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Check out this recipe!")
                putExtra(Intent.EXTRA_TEXT, "Try this delicious recipe: ${meal.strMeal}\nVideo: ${meal.strYoutube}")
            }
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        } ?: run {
            Toast.makeText(requireContext(), "No meal to share", Toast.LENGTH_SHORT).show()
        }
    }

    // Show a dialog to select a day for adding the meal to the plan
    private fun showAddToPlanDialog(meal: Meal) {
        val days = arrayOf("Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
        AlertDialog.Builder(requireContext())
            .setTitle("What day would you like to add ${meal.strMeal} ?")
            .setItems(days) { _, which ->
                val selectedDay = days[which]
                mealPlanViewModel.addMealToPlan(selectedDay, meal)
                Toast.makeText(requireContext(), "${meal.strMeal} added to $selectedDay", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Show a dialog for guest restrictions with an option to log in
    private fun showGuestRestrictionDialog(message: String) {
        CreateMaterialAlertDialogBuilder.createMaterialAlertDialogBuilderOkCancel(
            context = requireContext(),
            title = "Restricted Action",
            message = message,
            positiveBtnMsg = "Log In",
            negativeBtnMsg = "Cancel",
            positiveBtnFun = {
                findNavController().navigate(R.id.action_details_to_authActivity)
            }
        )
    }

    // Activate the YouTube player with the given video URL
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

    // Handle screen orientation changes to toggle fullscreen mode
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

    // Clean up resources when the view is destroyed
    override fun onDestroyView() {
        super.onDestroyView()
        Glide.with(this).clear(mealImage)
        youtubePlayerView.postDelayed({
            youtubePlayerView.release()
        }, 100)
    }
}