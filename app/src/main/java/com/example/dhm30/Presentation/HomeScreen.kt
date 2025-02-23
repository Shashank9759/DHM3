package com.example.dhm30.Presentation

import android.app.AppOpsManager
import android.provider.Settings
import com.example.dhm30.TrackingService
import android.Manifest
import android.app.Activity
import android.util.Log
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.auth.FirebaseAuth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dhm30.R
//import com.example.dhm30.TransitionReceiver
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext





@Preview()
@Composable
fun preview2(){
    val NavController= rememberNavController()
    val auth = FirebaseAuth.getInstance()
    HomeScreen(NavController,auth)
}
// Track if initial permissions are granted
var initialPermissionsGranted by mutableStateOf(false)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController, auth: FirebaseAuth) {
    val context = LocalContext.current
    // Initialize StateViewModel here
    val stateViewModel: StateViewModel = viewModel()




    // Observe the authState from the ViewModel
    val authState = stateViewModel.authState.collectAsState()
    val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

    // Initial permissions for location and audio (excluding activity recognition if < API 29)
    val initialPermissions = mutableListOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.FOREGROUND_SERVICE
    ).apply {
        // Add ACTIVITY_RECOGNITION only if API level >= 29
        add(Manifest.permission.ACTIVITY_RECOGNITION)
        add(Manifest.permission.BODY_SENSORS)
    }

    // Permission for background location
    val backgroundLocationPermission = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    // State for initial permissions and background permission
    val initialPermissionsState = rememberMultiplePermissionsState(permissions = initialPermissions)
    val backgroundPermissionState = rememberPermissionState(permission = backgroundLocationPermission)



    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    Button(onClick = {
                        sharedPreferences.edit().putBoolean("isFirstLaunch", true).apply()

                        auth.signOut()
                        navController.navigate("sign_in") {
                            popUpTo("home") { inclusive = true }
                        }
                    },colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Blue, // Background color of the button
                        contentColor = Color.White   // Text/Icon color inside the button
                    )) {
                        Row(verticalAlignment = Alignment.CenterVertically){
                            Icon(painter=painterResource(id = R.drawable.sign_out),contentDescription = "signout",
                                modifier = Modifier.size(25.dp))
                            Text(text="Sign Out",modifier=Modifier.padding(start=8.dp))
                        }

                    }
                }
            )
        }
    ) { innerPadding ->



            val navController2= rememberNavController()

        NavHost(modifier=Modifier.padding(innerPadding),navController=navController2,startDestination="permissionScreen"){
            composable("permissionScreen") {
                permissionScreen(initialPermissionsState,backgroundPermissionState,innerPadding,context,navController2)
            }


            composable("toggle_screen"){
                ActivityToggleScreen {
                        activity, isEnabled ->
               //     Toast.makeText(context,"${activity} , ${isEnabled.toString()}",
                  //      Toast.LENGTH_SHORT).show();
                }
            }


        }

    }
}

fun redirectToUsageAccessSettings(context: Context) {
    val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOpsManager.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    } else {
        appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
    }

    if (mode != AppOpsManager.MODE_ALLOWED) {
        Toast.makeText(
            context,
            "Usage Access is required. Please grant it to proceed.",
            Toast.LENGTH_LONG
        ).show()
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}


// Updated permissionScreen
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun permissionScreen(
    initialPermissionsState: MultiplePermissionsState,
    backgroundPermissionState: PermissionState,
    innerPadding: PaddingValues,
    context: Context,
    navController: NavHostController
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Initial Permissions
        if (initialPermissionsState.allPermissionsGranted) {
            initialPermissionsGranted = true
        }

        if (!initialPermissionsGranted) {
            // Request Initial Permissions
            if (initialPermissionsState.shouldShowRationale) {
                Text("Permissions are required for the app to function correctly.")
            } else {
                Text("Please allow the necessary permissions.")
            }

            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Blue,
                    contentColor = Color.White
                ),
                onClick = { initialPermissionsState.launchMultiplePermissionRequest() }
            ) {
                Text(text = "Request Initial Permissions")
            }
        } else if (!backgroundPermissionState.status.isGranted) {
            // Request Background Permission
            Text(
                textAlign = TextAlign.Center,
                text = "Background location permission is required for continuous access."
            )

            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Blue,
                    contentColor = Color.White
                ),
                onClick = { backgroundPermissionState.launchPermissionRequest() }
            ) {
                Text("Allow Background Location")
            }
        } else if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Handle Foreground Permissions (Android 13+)
            val foregroundPermissions = listOf(
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.FOREGROUND_SERVICE
            )
            val missingForegroundPermissions = foregroundPermissions.filter {
                ActivityCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }

            if (missingForegroundPermissions.isNotEmpty()) {
                Text("Foreground permissions are required for the app to function correctly.")

                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Blue,
                        contentColor = Color.White
                    ),
                    onClick = {
                        ActivityCompat.requestPermissions(
                            context as Activity,
                            missingForegroundPermissions.toTypedArray(),
                            1001
                        )
                    }
                ) {
                    Text("Allow Foreground Permissions")
                }
            } else {
                Text("All permissions granted!")
                LaunchedEffect(Unit) {
                    redirectToUsageAccessSettings(context)
                    startTrackingService(context)
                    delay(3000)
                    navController.navigate("toggle_screen")
                }
            }
        } else {
            Text("All permissions granted!")
            LaunchedEffect(Unit) {
                redirectToUsageAccessSettings(context)
                startTrackingService(context)
                delay(3000)
                navController.navigate("toggle_screen")
            }
        }
    }
}

fun startTrackingService(context: Context) {
    val serviceIntent = Intent(context, TrackingService::class.java)
    if (SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(serviceIntent)
        Log.d("vd", "Service started (Foreground)")
    } else {
        context.startService(serviceIntent)
        Log.d("vd", "Service started (Legacy)")
    }
}
