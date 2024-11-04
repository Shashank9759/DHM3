package com.example.dhm20

import android.os.Bundle
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
import com.example.dhm20.ui.theme.DHM20Theme
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth


    // StateFlow to hold the authentication state
    private val _authState = MutableStateFlow<Boolean>(false)
    val authState = _authState.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Check if user is already logged in
        if (auth.currentUser != null) {
            _authState.value = true // User is signed in
        }

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
