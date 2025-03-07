package com.example.foodplanner.main.view

import android.os.Bundle
import android.content.Intent
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import androidx.room.Room
import com.example.foodplanner.Fragments.FavoritesFragment
import com.example.foodplanner.home.view.HomeFragment
import com.example.foodplanner.Fragments.MealPlanFragment
import com.example.foodplanner.Fragments.SearchFragment
import com.example.foodplanner.R
import com.example.foodplanner.auth.AuthActivity
import com.example.foodplanner.auth.AuthViewModel
import com.example.foodplanner.auth.AuthViewModelFactory
import com.example.foodplanner.auth.Login.view.LoginFragment
import com.example.foodplanner.core.model.local.repository.UserRepositoryImpl
import com.example.foodplanner.core.model.local.source.LocalDataSourceImpl
import com.example.foodplanner.core.model.local.source.UserDatabase
import com.example.foodplanner.core.model.remote.Response
import com.example.foodplanner.core.util.CreateMaterialAlertDialogBuilder.createFailureResponse
import com.example.foodplanner.core.util.CreateMaterialAlertDialogBuilder.createMaterialAlertDialogBuilderOkCancel
import com.example.foodplanner.main.viewModel.RecipeActivityViewModel
import com.example.foodplanner.main.viewModel.RecipeActivityViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.navigation.NavigationView.OnNavigationItemSelectedListener
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), OnNavigationItemSelectedListener {

//    private lateinit var auth: FirebaseAuth
//    private lateinit var mGoogleSignInClient: GoogleSignInClient

    private lateinit var authViewModel: AuthViewModel
    private lateinit var db: UserDatabase
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var drawer: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle
    private var headerView: View? = null
    private lateinit var userName: TextView
    private lateinit var userEmail: TextView

    private var navController: NavController? = null



    private val navOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.slide_in_right)
        .setPopExitAnim(R.anim.slide_out_right)
        .build()

    override fun onResume() {
        super.onResume()
        Log.d("TAG", "onResume Called")
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)


        initUi()

        Log.d("TAG", "onCreate Called")

        /*db = Room.databaseBuilder(applicationContext, UserDatabase::class.java, "food-planner_database")
            .allowMainThreadQueries() // يفضل استخدام Coroutines بدلاً من هذا
            .build()

        lifecycleScope.launch {
            val userDao = db.userDao()
            val user = userDao.getLoggedInUser() // استدعاء دالة معلّقة داخل Coroutine

            if (user == null) {
                startActivity(Intent(this@MainActivity, AuthActivity::class.java))
                finish()
            }
        }*/

        val userRepository = UserRepositoryImpl(
            LocalDataSourceImpl(UserDatabase.getDatabaseInstance(this).userDao()),
            FirebaseAuth.getInstance()
        )

        val authViewModelFactory = AuthViewModelFactory(userRepository)
        authViewModel = ViewModelProvider(this, authViewModelFactory)[AuthViewModel::class.java]
        authViewModel.checkUserLoggedIn()

        authViewModel.user.observe(this) { user ->
            Log.d("AuthDebug", "User value: $user")
            if (user == null) {
                Log.d("AuthDebug", "Navigating to AuthActivity")
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
            } else {

                authViewModel.saveUserToLocalDatabase(user)

            }
        }

    }

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

        navController =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment)?.findNavController()

        //navigate by its own
        navController?.let { bottomNavigationView.setupWithNavController(it) }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        drawer.closeDrawer(GravityCompat.START)

        when (item.itemId) {
            R.id.action_profile -> {

                navController?.navigate(R.id.action_profile, null, navOptions)

                return true
            }

            R.id.action_settings -> {
                Toast.makeText(this, "Settings selected", Toast.LENGTH_SHORT).show()
                return true
            }

            R.id.action_dark_mode -> {
                Toast.makeText(this, "Dark Mode selected", Toast.LENGTH_SHORT).show()
                return true
            }

            R.id.action_security_policies -> {
                Toast.makeText(this, "Security Policies selected", Toast.LENGTH_SHORT).show()
                return true
            }

            R.id.action_about_developer -> {
                navController?.navigate(R.id.action_about_developer, null, navOptions)
                return true
            }

            R.id.action_about -> {
                navController?.navigate(R.id.action_about, null, navOptions)
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
                                is Response.Loading -> {}

                                is Response.Success -> {
                                    startActivity(Intent(this, AuthActivity::class.java))
                                    finish()
                                }

                                is Response.Failure -> {
                                    createFailureResponse(response, this)
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
                        authViewModel.deleteAccount()

                        authViewModel.deletedAccount.observe(this) { response ->
                            when (response) {
                                is Response.Loading -> {}

                                is Response.Success -> {
                                    startActivity(Intent(this, AuthActivity::class.java))
                                    finish()
                                }

                                is Response.Failure -> {
                                    createFailureResponse(response, this)
                                }
                            }
                        }
                    }
                )

                return true
            }

            else -> {
                return false
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        Log.d("TAG", "onDestroy Called")
    }
}



/*

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()


        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        // Check if user is logged in
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // If not logged in, redirect to LoginActivity
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        loadFragment(HomeFragment())

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_search -> loadFragment(SearchFragment())
                R.id.nav_favorites -> loadFragment(FavoritesFragment())
                R.id.nav_meal_plan -> loadFragment(MealPlanFragment())
            }
            true
        }
*/

/* // Logout Button
val logoutButton = findViewById<Button>(R.id.logoutButton)
logoutButton.setOnClickListener {
    mGoogleSignInClient.signOut()
    auth.signOut()
    startActivity(Intent(this, AuthActivity::class.java))
    finish()
}*/