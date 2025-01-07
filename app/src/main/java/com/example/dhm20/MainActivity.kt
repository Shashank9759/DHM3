package com.example.dhm20

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dhm20.Data.InternetConnectivityReceiver
import com.example.dhm20.Presentation.HomeScreen
import com.example.dhm20.Presentation.ScoreScreen
import com.example.dhm20.Presentation.SignInScreen
import com.example.dhm20.Presentation.StateViewModel
import com.example.dhm20.Presentation.SurveyScreen
import com.example.dhm20.ui.theme.DHM20Theme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.StateFlow
import androidx.navigation.NavBackStackEntry
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.dhm20.Utils.feelingsList
import java.util.concurrent.TimeUnit

var   auth = FirebaseAuth.getInstance()
class MainActivity : ComponentActivity() {


    private lateinit var internetConnectivityReceiver: InternetConnectivityReceiver


    // StateFlow to hold the authentication state

//    private val _authState = MutableStateFlow<Boolean>(false)
//    var authState = _authState.asStateFlow()

    // Initialize StateViewModel here
    private val viewmodel: StateViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)




        internetConnectivityReceiver = InternetConnectivityReceiver()

  //    auth = FirebaseAuth.getInstance()



        // Check if user is already logged in
        if (auth.currentUser != null) {
            if (shouldForceLogout(this)) { // Optional condition based on your logic
               // auth.signOut()
                viewmodel.updateAuthState(false) // Navigate to Login screen
            } else {
                viewmodel.updateAuthState(true) // User is signed in
            }
        }
     //  Log.d("logstate",viewmodel.updateAuthState().toString())




        setContent {
            DHM20Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(auth)
                }
            }
        }

        // Register the receiver dynamically
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(internetConnectivityReceiver, intentFilter)
    }

    override fun onStart() {
        super.onStart()
        // Register the receiver in onStart to ensure it's listening when the app is active
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        schedulePeriodicWork(this)
        registerReceiver(internetConnectivityReceiver, intentFilter)
    }

    fun schedulePeriodicWork(context: Context) {



    }


    private fun shouldForceLogout(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val isFirstLaunch = sharedPreferences.getBoolean("isFirstLaunch", true)

        if (isFirstLaunch) {
            // App was reinstalled or launched for the first time
            sharedPreferences.edit().putBoolean("isFirstLaunch", false).apply()
            return true // Force logout
        }
        return false // App is not reinstalled
    }

    override fun onStop() {
        super.onStop()
        // Unregister the receiver in onStop to avoid leaks when the app is not active
        unregisterReceiver(internetConnectivityReceiver)
    }
    @Composable
    fun AppNavigation( auth: FirebaseAuth) {
        val navController= rememberNavController()
        val isLoggedIn by viewmodel.authState.collectAsState()
      //  Toast.makeText(this,isLoggedIn.toString(),Toast.LENGTH_LONG).show()
        var startDestination:String;
        if(intent.hasExtra("Survey")){
            startDestination="survey"
             viewmodel.updateAuthState(true)

            }else{
            if (isLoggedIn){
                startDestination= "home"
            } else{
                startDestination=  "sign_in"
            }

        }



        NavHost(navController = navController, startDestination = startDestination) {
            composable("sign_in") { SignInScreen(navController) }
            composable("home") {
                HomeScreen(navController, auth)


            }

            composable("survey") {

                SurveyScreen(feelingsList,navController)


            }


            composable("score/{score}" ) { backStackEntry ->
                val score = backStackEntry.arguments?.getString("score")?.toIntOrNull() ?: 0
                ScoreScreen(score,navController)


            }

        }
    }


}
