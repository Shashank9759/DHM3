package com.example.dhm20

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class TransitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityTransitionResult.hasResult(intent)) {
            val result = ActivityTransitionResult.extractResult(intent)
            result?.transitionEvents?.forEach { event ->
                logTransitionEvent(event)
            }
        }
    }

    private fun logTransitionEvent(event: ActivityTransitionEvent) {
        // Get the current user's UID for individual data storage
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Reference to Firebase Database with user-specific path
        val database: DatabaseReference = FirebaseDatabase.getInstance().reference
            .child("users")
            .child(userId)
            .child("activity_transitions")

        // Determine activity and transition types
        val activityType = when (event.activityType) {
            DetectedActivity.IN_VEHICLE -> "In Vehicle"
            DetectedActivity.ON_BICYCLE -> "On Bicycle"
            DetectedActivity.ON_FOOT -> "On Foot"
            DetectedActivity.RUNNING -> "Running"
            DetectedActivity.STILL -> "Still"
            DetectedActivity.WALKING -> "Walking"
            else -> "Unknown"
        }
        val transitionType = if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) "Enter" else "Exit"

        // Timestamp for the event
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // Log event details
        val transitionData = mapOf(
            "activity" to activityType,
            "transition" to transitionType,
            "timestamp" to timestamp
        )

        // Push data to Firebase Database under the user's node
        database.push().setValue(transitionData)
            .addOnSuccessListener {
                Log.d("TransitionReceiver", "Logged $activityType $transitionType at $timestamp")
            }
            .addOnFailureListener {
                Log.e("TransitionReceiver", "Failed to log transition: ${it.message}")
            }
    }
}