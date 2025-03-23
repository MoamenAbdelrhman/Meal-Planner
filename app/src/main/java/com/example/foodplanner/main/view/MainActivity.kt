package com.example.foodplanner.main.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.foodplanner.R
import com.example.foodplanner.auth.AuthActivity
import com.example.foodplanner.auth.AuthViewModel
import com.example.foodplanner.auth.AuthViewModelFactory
import com.example.foodplanner.core.model.local.repository.UserRepositoryImpl
import com.example.foodplanner.core.model.local.source.LocalDataSourceImpl
import com.example.foodplanner.core.model.local.source.UserDatabase
import com.example.foodplanner.core.model.remote.Response
import com.example.foodplanner.core.model.remote.repository.MealRepositoryImpl
import com.example.foodplanner.core.model.remote.source.RemoteGsonDataImpl
import com.example.foodplanner.core.util.CreateMaterialAlertDialogBuilder.createMaterialAlertDialogBuilderOkCancel
import com.example.foodplanner.core.viewmodel.DataViewModel
import com.example.foodplanner.core.viewmodel.DataViewModelFactory
import com.example.foodplanner.main.viewModel.MainActivityViewModel
import com.example.foodplanner.main.viewModel.MainActivityViewModelFactory
import com.example.foodplanner.meal_plan.viewModel.MealPlanViewModel
import com.example.foodplanner.meal_plan.viewModel.MealPlanViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private val viewModel: MainActivityViewModel by viewModels {
        MainActivityViewModelFactory()
    }

    private val mealPlanViewModel: MealPlanViewModel by viewModels {
        val userRepository = UserRepositoryImpl(
            LocalDataSourceImpl(UserDatabase.getDatabaseInstance(this).userDao()),
            FirebaseAuth.getInstance()
        )
        MealPlanViewModelFactory(userRepository, this)
    }

    private val dataViewModel: DataViewModel by viewModels {
        val userRepository = UserRepositoryImpl(
            LocalDataSourceImpl(UserDatabase.getDatabaseInstance(this).userDao()),
            FirebaseAuth.getInstance()
        )
        val mealRepository = MealRepositoryImpl(RemoteGsonDataImpl())
        val favouriteMealDao = UserDatabase.getDatabaseInstance(this).favouriteMealDao()
        DataViewModelFactory(userRepository, mealRepository, favouriteMealDao)
    }

    private lateinit var authViewModel: AuthViewModel
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var drawer: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle
    private var headerView: View? = null
    private lateinit var userName: TextView
    private lateinit var userEmail: TextView
    private lateinit var navController: NavController
    private val auth = FirebaseAuth.getInstance()
    private var isGuest: Boolean = false
    private lateinit var authStateListener: AuthStateListener

    private val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_in_right)
        .setPopExitAnim(R.anim.slide_out_right)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        isGuest = intent.getBooleanExtra("IS_GUEST", false)
        initUi()

        navController = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment)!!
            .findNavController()

        bottomNavigationView.setupWithNavController(navController)
        navigationView.setNavigationItemSelectedListener(this)

        val userRepository = UserRepositoryImpl(
            LocalDataSourceImpl(UserDatabase.getDatabaseInstance(this).userDao()),
            FirebaseAuth.getInstance()
        )
        val authViewModelFactory = AuthViewModelFactory(userRepository)
        authViewModel = ViewModelProvider(this, authViewModelFactory)[AuthViewModel::class.java]

        // Set up Firebase Auth state listener to monitor user login state
        authStateListener = AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                isGuest = false
                dataViewModel.startFavoritesSync(user.uid)
                mealPlanViewModel.startMealPlansSync(user.uid)
                userName.text = firebaseAuth.currentUser?.displayName ?: "Unknown"
                userEmail.text = firebaseAuth.currentUser?.email ?: "No email"
                updateNavigationMenu()
            }
        }

        auth.addAuthStateListener(authStateListener)

        // Observe navigation events from ViewModel to update bottom navigation
        viewModel.navigateToFragment.observe(this) { fragmentId ->
            fragmentId?.let {
                bottomNavigationView.selectedItemId = it
            }
        }

        Log.d("TAG", "onCreate Called")
    }

    // Initialize UI components like bottom navigation, drawer, and navigation view
    private fun initUi() {
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        drawer = findViewById(R.id.drawer)
        navigationView = findViewById(R.id.navigation_view)

        headerView = navigationView.getHeaderView(0)
        headerView?.let {
            userName = it.findViewById(R.id.userNameDrawer)
            userEmail = it.findViewById(R.id.userEmailDrawer)
        }

        toggle = ActionBarDrawerToggle(
            this, drawer,
            R.string.drawer_opened, R.string.drawer_closed
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        navigationView.setNavigationItemSelectedListener(this)
        updateNavigationMenu()
        navController = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment)!!
            .findNavController()

        bottomNavigationView.setupWithNavController(navController)

        if (isGuest) {
            bottomNavigationView.menu.findItem(R.id.action_favourite).isVisible = false
            bottomNavigationView.menu.findItem(R.id.action_meal_plan).isVisible = false
        }

        bottomNavigationView.selectedItemId = R.id.nav_home

        // Hide bottom navigation for specific fragments
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.mealsFragment, R.id.action_details -> {
                    bottomNavigationView.visibility = View.GONE
                }
                else -> {
                    bottomNavigationView.visibility = View.VISIBLE
                }
            }
        }
    }

    // Update navigation menu visibility based on guest status
    private fun updateNavigationMenu() {
        val menu = navigationView.menu
        menu.findItem(R.id.action_login)?.isVisible = isGuest
        menu.findItem(R.id.action_log_out)?.isVisible = !isGuest
        menu.findItem(R.id.action_delete_account)?.isVisible = !isGuest
    }

    // Handle navigation item selection from the drawer
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        drawer.closeDrawer(GravityCompat.START)

        if (isGuest && (item.itemId == R.id.action_favourite || item.itemId == R.id.action_meal_plan)) {
            Toast.makeText(
                this,
                "Guests cannot access this feature. Please log in.",
                Toast.LENGTH_SHORT
            ).show()
            return true
        }

        when (item.itemId) {
            R.id.action_login -> {
                navigateToAuthActivity(clearTask = false)
                return true
            }

            R.id.action_about_developer -> {
                navController.navigate(R.id.action_about_developer, null, navOptions)
                return true
            }

            R.id.action_about -> {
                navController.navigate(R.id.action_about, null, navOptions)
                return true
            }

            R.id.action_log_out -> {
                createMaterialAlertDialogBuilderOkCancel(
                    context = this,
                    title = "Log Out",
                    message = "Are you sure you want to log out?",
                    positiveBtnMsg = "Log Out",
                    negativeBtnMsg = "Cancel",
                    positiveBtnFun = {
                        authViewModel.logOut()
                        authViewModel.loggedOut.observe(this) { response ->
                            when (response) {
                                is Response.Loading -> {
                                    Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show()
                                }
                                is Response.Success -> {
                                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
                                    isGuest = true
                                    navigateToAuthActivity(clearTask = true, isGuest = true)
                                }
                                is Response.Failure -> {
                                    Toast.makeText(this, "Failed to log out: ${response.reason}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                )
                return true
            }

            R.id.action_delete_account -> {
                createMaterialAlertDialogBuilderOkCancel(
                    context = this,
                    title = "Delete Account",
                    message = "This action will permanently delete your account. Are you sure you want to continue?",
                    positiveBtnMsg = "Delete",
                    negativeBtnMsg = "Cancel",
                    positiveBtnFun = {
                        val userId = auth.currentUser?.uid ?: run {
                            Toast.makeText(this, "No user is signed in", Toast.LENGTH_SHORT).show()
                            return@createMaterialAlertDialogBuilderOkCancel
                        }
                        mealPlanViewModel.clearMealPlanForUser(userId)
                        mealPlanViewModel.clearMealPlanResult.observe(this) { success ->
                            if (success) {
                                authViewModel.deleteAccount()
                                authViewModel.deletedAccount.observe(this) { response ->
                                    when (response) {
                                        is Response.Loading -> {
                                            Toast.makeText(this, "Deleting account...", Toast.LENGTH_SHORT).show()
                                        }
                                        is Response.Success -> {
                                            Toast.makeText(this, "Account deleted successfully", Toast.LENGTH_SHORT).show()
                                            isGuest = true
                                            navigateToAuthActivity(clearTask = true, isGuest = true)
                                        }
                                        is Response.Failure -> {
                                            Toast.makeText(this, "Failed to delete account: ${response.reason}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            } else {
                                Toast.makeText(this, "Failed to clear meal plans", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
                return true
            }

            else -> {
                navController.navigate(item.itemId)
                return true
            }
        }
    }

    // Navigate to AuthActivity for login, optionally clearing the task stack
    private fun navigateToAuthActivity(clearTask: Boolean = false, isGuest: Boolean = false) {
        dataViewModel.stopFavoritesSync()
        mealPlanViewModel.stopMealPlansSync()
        val intent = Intent(this, AuthActivity::class.java).apply {
            if (clearTask) {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            putExtra("IS_GUEST", isGuest)
        }
        startActivity(intent)
        if (clearTask) {
            finish()
        }
    }

    // Clean up by removing the Firebase Auth state listener
    override fun onDestroy() {
        super.onDestroy()
        auth.removeAuthStateListener(authStateListener)
        Log.d("MainActivity", "AuthStateListener removed in onDestroy")
    }
}