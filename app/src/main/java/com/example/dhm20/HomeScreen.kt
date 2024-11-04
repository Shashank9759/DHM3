package com.example.dhm20

import com.example.dhm20.TrackingService
import android.Manifest
import android.app.Activity
import android.util.Log
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore


@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController, auth: FirebaseAuth) {
    val context = LocalContext.current
    val firebaseDatabase = FirebaseDatabase.getInstance().reference

    // Initial permissions for location and audio (excluding activity recognition if < API 29)
    val initialPermissions = mutableListOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.POST_NOTIFICATIONS,
        Manifest.permission.READ_PHONE_STATE
    ).apply {
        // Add ACTIVITY_RECOGNITION only if API level >= 29
        add(Manifest.permission.ACTIVITY_RECOGNITION)
    }

    // Permission for background location
    val backgroundLocationPermission = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    // State for initial permissions and background permission
    val initialPermissionsState = rememberMultiplePermissionsState(permissions = initialPermissions)
    val backgroundPermissionState = rememberPermissionState(permission = backgroundLocationPermission)

    // Track if initial permissions are granted
    var initialPermissionsGranted by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard") },
                actions = {
                    Button(onClick = {
                        auth.signOut()
                        navController.navigate("sign_in") {
                            popUpTo("home") { inclusive = true }
                        }
                    }) {
                        Text("Sign Out")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            if (initialPermissionsState.allPermissionsGranted) {
                initialPermissionsGranted = true
            }

            if (!initialPermissionsGranted) {
                // Initial permissions are not granted
                if (initialPermissionsState.shouldShowRationale) {
                    Text("Permissions are required for the app to function correctly.")
                } else {
                    Text("Please allow the necessary permissions.")
                }

                Button(onClick = { initialPermissionsState.launchMultiplePermissionRequest() }) {
                    Text("Request Initial Permissions")
                }
            } else if (!backgroundPermissionState.status.isGranted) {
                // Ask for background permission after initial permissions are granted
                Text("Background location permission is required for continuous access.")

                Button(onClick = { backgroundPermissionState.launchPermissionRequest() }) {
                    Text("Allow Background Location")
                }

            }else if (SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
                }
            }else{
                Text("All permissions granted!")
                // Automatically start the tracking service once all permissions are granted
                val serviceIntent = Intent(context, TrackingService::class.java)
                if (SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                    startActivityTransitionUpdates(context )
                    Log.d("vd","service started")
                } else {
                    Log.d("vd","service started2")
                    context.startService(serviceIntent)
//                    startActivityTransitionUpdates(context)
                }
            }

        }
    }
}


fun startActivityTransitionUpdates(context: Context) {
    val transitions = listOf(
        ActivityTransition.Builder().setActivityType(DetectedActivity.IN_VEHICLE)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build(),
        ActivityTransition.Builder().setActivityType(DetectedActivity.IN_VEHICLE)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build(),
        ActivityTransition.Builder().setActivityType(DetectedActivity.WALKING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build(),
        ActivityTransition.Builder().setActivityType(DetectedActivity.WALKING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build(),
        ActivityTransition.Builder().setActivityType(DetectedActivity.RUNNING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build(),
        ActivityTransition.Builder().setActivityType(DetectedActivity.RUNNING)
            .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build()
    )

    val request = ActivityTransitionRequest(transitions)
    val intent = Intent(context, TransitionReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    )

    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
        val task = ActivityRecognition.getClient(context).requestActivityTransitionUpdates(request, pendingIntent)
        task.addOnSuccessListener {
            Log.d("ActivityTransition", "Activity transitions successfully registered.")
        }
        task.addOnFailureListener {
            Log.e("ActivityTransition", "Failed to register activity transitions: ${it.message}")
        }
    }
}