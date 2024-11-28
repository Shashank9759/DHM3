package com.example.dhm20


import androidx.compose.ui.platform.LocalContext
import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.Service
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.example.dhm20.Data.ActivityLog
import com.example.dhm20.Data.AppDatabase
import com.example.dhm20.Presentation.StateViewModel
import com.example.dhm20.Presentation.toggleStates
import com.example.dhm20.R
import com.example.dhm20.SleepReceiver.Companion.logSleepEvent
import com.example.dhm20.TransitionReceiver2.Companion.logTransitionEvent
//import com.example.dhm20.TransitionReceiver
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransition.ACTIVITY_TRANSITION_ENTER
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.SleepSegmentEvent
import com.google.android.gms.location.SleepSegmentRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



class TrackingService() : Service() {
    private val firebaseDatabase = FirebaseDatabase.getInstance().reference
    lateinit var notification:Notification


    val notificationId = 404
    val handler=Handler(Looper.getMainLooper())
    var count=0
    override fun onCreate() {
        super.onCreate()
        startForegroundServiceWithNotification()

        startActivityTransitionUpdates(this)
        requestSleepUpdates(this)




    }


    private fun startForegroundServiceWithNotification() {
        val channelId = "tracking_service_channel"


        val channel = NotificationChannel(
            channelId,
            "Tracking Service",
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
        Log.d("TrackingService", "Notification channel created")

         notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Monitoring Activity")
            .setContentText("Collecting activity transition data")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()

        Log.d("TrackingService", "Attempting to start foreground")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
            startForeground(
                notificationId, // Notification ID
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                or    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                or    ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH

            )
        } else {
            startForeground(notificationId, notification) // For older versions
        }

        Log.d("TrackingService", "Foreground started")
      //  startForeground(notificationId, notification)
     //   startActivityTransitionUpdates(this)
        Toast.makeText(this, "Foreground service started", Toast.LENGTH_SHORT).show()

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
Log.d("onstartcommenad","working")
        val trans=ActivityTransitionEvent(DetectedActivity.RUNNING,ACTIVITY_TRANSITION_ENTER,0L)
        val sleepEvent = SleepSegmentEvent(1625000000000L, 1625030000000L, SleepSegmentEvent.STATUS_SUCCESSFUL, 0,0)


        logTransitionEvent(trans,this)
//        logTransitionEvent(trans,this)
//        logSleepEvent(sleepEvent,this)

//        val toastRunnable=object:Runnable{
//            override fun run() {
//                Toast.makeText(this@TrackingService,"${count}",Toast.LENGTH_SHORT).show()
//                count++
//                handler.postDelayed(this,1000)
//            }
//
//        }
//        handler.postDelayed(toastRunnable,1000)
        return START_STICKY
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
        val intent = Intent(context, TransitionReceiver2::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            val task = ActivityRecognition.getClient(context).requestActivityTransitionUpdates(request, pendingIntent)
            task.addOnSuccessListener {
                Log.d("ActivityTransition2", "Activity transitions successfully registered.")
            }
            task.addOnFailureListener {
                Log.e("ActivityTransition2", "Failed to register activity transitions: ${it.message}")
            }
        }
    }
    private fun requestSleepUpdates(context: Context) {
        // Check if the ACTIVITY_RECOGNITION permission is granted
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            try {
                val sleepPendingIntent = PendingIntent.getBroadcast(
                    context,
                    1,
                    Intent(context, SleepReceiver::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                val sleepSegmentRequest = SleepSegmentRequest.getDefaultSleepSegmentRequest()
                val task = ActivityRecognition.getClient(context).requestSleepSegmentUpdates(sleepPendingIntent,sleepSegmentRequest)

                task.addOnSuccessListener {
                    Log.d("SleepAPI", "Sleep updates successfully registered.")
                }.addOnFailureListener {
                    Log.e("SleepAPI", "Failed to register sleep updates: ${it.message}")
                }
            } catch (e: SecurityException) {
                Log.e("SleepAPI", "SecurityException: ${e.message}")
            }
        } else {
            // Permission not granted, request permission
            ActivityCompat.requestPermissions(
                (context as Activity),
                arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION),
                REQUEST_CODE_ACTIVITY_RECOGNITION
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}



class TransitionReceiver2 : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityTransitionResult.hasResult(intent)) {
            Log.d("contentReceived","received")
          //  Toast.makeText(context,"contentReceived",Toast.LENGTH_SHORT).show()
            val result = ActivityTransitionResult.extractResult(intent)
            result?.transitionEvents?.forEach { event ->


                logTransitionEvent(event,context)
            }
            if(result?.transitionEvents?.get(0)==null){
                Log.d("events is getting","null")
            }else{
                Log.d("events is getting","not null")
            }
        }
    }
companion object{
    fun logTransitionEvent(event: ActivityTransitionEvent,context: Context) {
        val db = AppDatabase.getInstance(context)
        val dao = db.activityLogDao()
        Log.d("@@@@","worked1")
    //    Toast.makeText(context,"worked1",Toast.LENGTH_SHORT).show()
        // Get the current user's UID for individual data storage
        //    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

//        // Reference to Firebase Database with user-specific path
//        val database: DatabaseReference = FirebaseDatabase.getInstance().reference
//            .child("users")
//            .child(userId)
//            .child("activity_transitions")
//
//        // Determine activity and transition types
//        val activityType = when (event.activityType) {
//            DetectedActivity.IN_VEHICLE -> "In Vehicle"
//            DetectedActivity.ON_BICYCLE -> "On Bicycle"
//            DetectedActivity.ON_FOOT -> "On Foot"
//            DetectedActivity.RUNNING -> "Running"
//            DetectedActivity.STILL -> "Still"
//            DetectedActivity.WALKING -> "Walking"
//            else -> "Unknown"
//        }
//        val transitionType = if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) "Enter" else "Exit"
//
//        // Timestamp for the event
//        val timestamp = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
//
//        // Log event details
//        val transitionData = mapOf(
//            "activity" to activityType,
//            "transition" to transitionType,
//            "timestamp" to timestamp
//        )
//        Log.d("@@@@","worked2")
//         Push data to Firebase Database under the user's node
//        database.push().setValue(transitionData)
//            .addOnSuccessListener {
//                Log.d("TransitionReceiver2", "Logged $activityType $transitionType at $timestamp")
//            }
//            .addOnFailureListener {
//                Log.e("TransitionReceiver2", "Failed to log transition: ${it.message}")
//            }


        toggleStates


        val activityType=when (event.activityType) {
            DetectedActivity.IN_VEHICLE -> "Vehicle"
            DetectedActivity.RUNNING -> "Running"
            DetectedActivity.WALKING -> "Walking"
            else -> "Unknown"
        }

        if(!(toggleStates[activityType]?:false)){
       //     Toast.makeText(context,"Receiver ${activityType} , ${toggleStates[activityType].toString()}",
           //     Toast.LENGTH_SHORT).show();
             return
        }
        val log = ActivityLog(
            activityType = activityType,
            transitionType = if (event.transitionType == ActivityTransition.ACTIVITY_TRANSITION_ENTER) "Enter" else "Exit",
            timestamp = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        )
        CoroutineScope(Dispatchers.IO).launch {
            dao.insert(log)
        }
        Log.d("@@@@","worked3")
    }
}

}