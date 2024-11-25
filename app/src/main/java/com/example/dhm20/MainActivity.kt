package com.example.dhm20

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.dhm20.Data.InternetConnectivityReceiver
import com.example.dhm20.Data.SyncWorker
import com.example.dhm20.ui.theme.DHM20Theme
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var internetConnectivityReceiver: InternetConnectivityReceiver

    // StateFlow to hold the authentication state
    private val _authState = MutableStateFlow<Boolean>(false)
    val authState = _authState.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        internetConnectivityReceiver = InternetConnectivityReceiver()
        auth = FirebaseAuth.getInstance()

        // Check if user is already logged in
        if (auth.currentUser != null) {
            if (shouldForceLogout(this)) { // Optional condition based on your logic
                auth.signOut()
                _authState.value = false // Navigate to Login screen
            } else {
                _authState.value = true // User is signed in
            }
        }
        Log.d("logstate",_authState.value.toString())




        setContent {
            DHM20Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(authState, auth)
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
        registerReceiver(internetConnectivityReceiver, intentFilter)
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


}

@Composable
fun AppNavigation(authState: StateFlow<Boolean>, auth: FirebaseAuth) {
    val navController: NavHostController = rememberNavController()
    val isLoggedIn by authState.collectAsState()

    NavHost(navController = navController, startDestination = if (isLoggedIn) "home" else "sign_in") {
        composable("sign_in") { SignInScreen(navController, auth) }
        composable("home") { HomeScreen(navController, auth) }
    }
}