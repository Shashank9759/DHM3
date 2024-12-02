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
import android.content.SharedPreferences

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.*
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.*
import com.example.dhm20.Data.AudioDB
import com.example.dhm20.Data.AudioLog
import com.example.dhm20.Data.LocationDB
import com.google.android.gms.location.*
import com.google.android.gms.location.FusedLocationProviderClient

import com.google.android.gms.location.SleepClassifyEvent
import java.util.*
import com.example.dhm20.Data.LocationLog
data class AppUsageData(var openCount: Int = 0, var totalDuration: Long = 0)

private var currentActivity = "Still"


class TrackingService() : Service() {
    private val firebaseDatabase = FirebaseDatabase.getInstance().reference
    lateinit var notification:Notification

    //dev
    private lateinit var usageStatsManager: UsageStatsManager
    private val dailyAppUsage = mutableMapOf<String, AppUsageData>()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var screenOnTime = 0L
    private var screenOnStartTime = 0L
    private val screenReceiver = ScreenReceiver()
    private  val syncInterval= 5000L
    //devend

    val notificationId = 404
    val handler=Handler(Looper.getMainLooper())
    var count=0
    override fun onCreate() {
        super.onCreate()
        startForegroundServiceWithNotification()

        startActivityTransitionUpdates(this)
        requestSleepUpdates(this)

        //dev
        initializeServices()
        Log.d("TrackingService", "onCreate: Scheduling daily sync")
        scheduleDailyNotification()
        Log.d("TrackingService", "onCreate: Starting service")
        scheduleDailySyncAt11PM()
        Log.d("TrackingService", "onCreate: Registering ScreenReceiver")
        registerScreenReceiver()
        Log.d("TrackingService", "onCreate: Service initialization complete")
        //devend

    }
    //dev
    private fun initializeServices() {
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        Log.d("TrackingService", "initializeServices: UsageStatsManager initialized")
    }

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)
        Log.d("TrackingService", "registerScreenReceiver: ScreenReceiver registered")
    }
    private fun unregisterScreenReceiver() {
        unregisterReceiver(screenReceiver)
        Log.d("TrackingService", "unregisterScreenReceiver: ScreenReceiver unregistered")
    }

    //devend

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


        //dev
        startActivityTransitionUpdates(this)
        startPeriodicSync()
        startAudioDetection()
        startLocationUpdates()


        if (intent?.getBooleanExtra("run_sync", false) == true) {
            aggregateAppUsageData()
            syncAppUsageDataToFirebase()
        }
        return START_STICKY
        //devend
    }


    //dev
    private inner class ScreenReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> {
                    screenOnStartTime = System.currentTimeMillis()
                    Log.d("ScreenReceiver", "onReceive: Screen ON detected at $screenOnStartTime")
                }
                Intent.ACTION_SCREEN_OFF -> {
                    if (screenOnStartTime != 0L) {
                        screenOnTime += System.currentTimeMillis() - screenOnStartTime
                        screenOnStartTime = 0L
                        Log.d("ScreenReceiver", "onReceive: Screen OFF. Accumulated screen time: $screenOnTime ms")
                    }
                }
            }
        }
    }
    private fun aggregateAppUsageData() {
        Log.d("TrackingService", "aggregateAppUsageData: Aggregating app usage data")
        val startTime = System.currentTimeMillis() - AlarmManager.INTERVAL_DAY
        Log.d("TrackingService", "aggregateAppUsageData: Querying usage stats from $startTime")
        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, System.currentTimeMillis()
        )
        for (usageStat in usageStatsList) {
            Log.d("TrackingService", "aggregateAppUsageData: Processing app ${usageStat.packageName}")
            val appUsageData = dailyAppUsage.getOrPut(usageStat.packageName) { AppUsageData() }
            if (usageStat.totalTimeInForeground > 0) {
                appUsageData.openCount += 1
                appUsageData.totalDuration += usageStat.totalTimeInForeground
                Log.d(
                    "TrackingService", "aggregateAppUsageData: App ${usageStat.packageName} - " +
                            "Opened: ${appUsageData.openCount}, Total Duration: ${appUsageData.totalDuration} ms"
                )
            }
        }
    }
    private fun syncAppUsageDataToFirebase() {
        Log.d("TrackingService", "syncAppUsageDataToFirebase: Starting data sync")
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Log.w("TrackingService", "syncAppUsageDataToFirebase: User ID is null, aborting sync")
            return
        }
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val userRef = firebaseDatabase.child("users").child(userId).child("phone_usage").child(date)

        Log.d("TrackingService", "syncAppUsageDataToFirebase: Syncing screen time: $screenOnTime ms")
        userRef.child("screen_time").setValue(screenOnTime)

        dailyAppUsage.forEach { (packageName, appData) ->
            val simplifiedAppName = simplifyAppName(packageName)
            Log.d(
                "TrackingService", "syncAppUsageDataToFirebase: App $simplifiedAppName - " +
                        "Opened: ${appData.openCount} times, Duration: ${appData.totalDuration} ms"
            )
            val appRef = userRef.child("apps").child(simplifiedAppName)
            appRef.child("opened").setValue(appData.openCount)
            appRef.child("aggregated_duration").setValue(appData.totalDuration)
        }

        Log.d("TrackingService", "syncAppUsageDataToFirebase: Data sync complete for user $userId")
    }

    private fun simplifyAppName(packageName: String): String {
        // Map known apps to their user-friendly names
        val knownApps = mapOf(
            "com.snapchat.android" to "Snapchat",
            "com.linkedin.android" to "LinkedIn",
            "com.instagram.android" to "Instagram",
            "com.twitter.android" to "Twitter",
            "org.telegram.messenger" to "Telegram",
            "com.bereal.ft" to "BeReal"
        )

        // Check if the package is in the map, otherwise use the default logic
        return knownApps[packageName] ?: packageName.substringAfterLast(".")
    }
    private fun scheduleDailySyncAt11PM() {
        Log.d("TrackingService", "scheduleDailySyncAt11PM: Setting up daily sync alarm")
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, SyncReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        Log.d("TrackingService", "scheduleDailySyncAt11PM: Alarm set for ${calendar.time}")
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
        Log.d("TrackingService", "scheduleDailySyncAt11PM: Daily sync alarm scheduled")
    }

    private fun startPeriodicSync() {
        handler.postDelayed({ syncActivityData(currentActivity) }, syncInterval)
    }

    private fun syncActivityData(activityType: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firebaseDatabase.child("users").child(userId).child("activity_transitions").push().setValue(
            mapOf(
                "activity" to activityType,
                "timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            )
        )
    }

    private fun scheduleDailyNotification() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set the time for 7 PM
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 19) // 7 PM in 24-hour format
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        // Schedule the alarm to repeat every day at 7 PM
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun startAudioDetection() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return
        val bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize).apply { startRecording() }
        isRecording = true
        Thread { continuouslyMonitorAudio(bufferSize) }.start()
    }

    private fun continuouslyMonitorAudio(bufferSize: Int) {
        val audioBuffer = ShortArray(bufferSize)
        while (isRecording) {
            val readSize = audioRecord?.read(audioBuffer, 0, bufferSize) ?: 0
            if (readSize > 0) {
                syncAudioState((audioBuffer.maxOrNull()?.toInt() ?: 0) > CONVERSATION_THRESHOLD)
                Thread.sleep(1000)
            }
        }
    }

    private fun syncAudioState(isConversationDetected: Boolean) {
        val db = AudioDB.getInstance(this)
        val dao = db.audiologDao()
        if(!(toggleStates["Microphone"]?:false)){
            //     Toast.makeText(context,"Receiver ${activityType} , ${toggleStates[activityType].toString()}",
            //     Toast.LENGTH_SHORT).show();
            return
        }
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
//        firebaseDatabase.child("users").child(userId).child("audio_data").push().setValue(
//            mapOf(
//                "conversation" to if (isConversationDetected) 1 else 0,
//                "timestamp" to SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
//            )
//        )

        val log = AudioLog(
            conversation =if (isConversationDetected) 1 else 0,

            timestamp = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        )
        Log.d("audio_log",log.toString())
        CoroutineScope(Dispatchers.IO).launch {
            dao.insert(log)
        }
    }

    private fun startLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 60000).build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    logLocationData(location.latitude, location.longitude)
                }
            }
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, mainLooper)
        }
    }

    private fun logLocationData(latitude: Double, longitude: Double) {
        val db = LocationDB.getInstance(this)
        val dao = db.locationlogDao()
        if(!(toggleStates["Location"]?:false)){
            //     Toast.makeText(context,"Receiver ${activityType} , ${toggleStates[activityType].toString()}",
            //     Toast.LENGTH_SHORT).show();
            return
        }
  //      val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
//        firebaseDatabase.child("users").child(userId).child("gps_data").push().setValue(
//            mapOf(
//                "latitude" to latitude,
//                "longitude" to longitude,
//                "timestamp" to SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
//            )
//        )
        val log = LocationLog(
            latitude =latitude,
             longitude = longitude,
            timestamp = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        )
        CoroutineScope(Dispatchers.IO).launch {
            dao.insert(log)
        }
    }


    //devend


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
        Intent(context, SleepReceiver::class.java)
        // Check if the ACTIVITY_RECOGNITION permission is granted
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            try {
                val sleepPendingIntent = PendingIntent.getBroadcast(
                    context,
                    1,
                    Intent(context, SleepReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
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

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        audioRecord?.apply { stop(); release() }
        isRecording = false
        handler.removeCallbacksAndMessages(null)
        unregisterScreenReceiver()


    }

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val CONVERSATION_THRESHOLD = 2000
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}


class SyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // Start the service to execute the sync function at 11 PM
        context?.let {
            val serviceIntent = Intent(it, TrackingService::class.java)
            serviceIntent.putExtra("run_sync", true) // Extra flag to trigger sync
            it.startService(serviceIntent)
            Log.d("SyncReceiver", "Triggered daily sync at 11 PM")
        }
    }
}


class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.let {
            val notificationManager = it.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "daily_test_notification_channel"

            // Create notification channel if it doesn't exist (for Android 8.0 and above)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Daily Test Notification",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminder to take your daily test"
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Create the notification
            val notification = NotificationCompat.Builder(it, channelId)
                .setSmallIcon(R.drawable.ic_notification) // Replace with your app's notification icon
                .setContentTitle("Time for Your Daily Test")
                .setContentText("Please complete your daily health test.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            // Show the notification
            notificationManager.notify(1001, notification)
        }
    }
}


class RebootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            context?.let {
                // Start TrackingService to reschedule the notification
                val trackingServiceIntent = Intent(it, TrackingService::class.java)
                it.startService(trackingServiceIntent)

                // Start any other foreground services needed, such as location updates or audio recording
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.startForegroundService(trackingServiceIntent)
                } else {
                    it.startService(trackingServiceIntent)
                }

                Log.d("RebootReceiver", "Foreground services started after reboot")
            }
        }
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