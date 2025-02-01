package com.example.dhm20


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
import com.example.dhm20.Data.Entities.ActivityLog
import com.example.dhm20.Data.Database.AppDatabase
import com.example.dhm20.Presentation.toggleStates
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
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.app.*
import android.app.usage.UsageStatsManager
import android.content.*
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.example.dhm20.Data.Database.AppUsageDB
import com.example.dhm20.Data.Entities.AppUsageLog
import com.example.dhm20.Data.Database.AudioDB
import com.example.dhm20.Data.Entities.AudioLog
import com.example.dhm20.Data.Database.LocationDB
import com.google.android.gms.location.*
import com.google.android.gms.location.FusedLocationProviderClient
import android.provider.Settings

import java.util.*
import com.example.dhm20.Data.Entities.LocationLog
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
    private var screenOnEndTime = 0L

    private val screenReceiver = ScreenReceiver()
    private  val syncInterval= 5000L
    //devend

    val notificationId = 404
    val handler=Handler(Looper.getMainLooper())
    var count=0

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceWithNotification()



        //dev
        initializeServices()
        Log.d("TrackingService", "onCreate: Scheduling daily sync")
        scheduleDailyNotifications()
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
        val notificationManager = getSystemService(NotificationManager::class.java)

        // Create Notification Channel
        val channel = NotificationChannel(
            channelId,
            "Tracking Service",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        // Intent to reopen the app when clicked
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Base Notification
        val baseNotification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Monitoring Activity")
            .setContentText("Collecting activity transition data")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Start Foreground Service Notification
        val notification = baseNotification
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
            startForeground(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            )
        } else {
            startForeground(notificationId, notification)
        }

        Log.d("TrackingService", "Foreground service started with notifications")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    Log.d("onstartcommenad","working")
        val trans=ActivityTransitionEvent(DetectedActivity.RUNNING,ACTIVITY_TRANSITION_ENTER,0L)
        val sleepEvent = SleepSegmentEvent(1625000000000L, 1625030000000L, SleepSegmentEvent.STATUS_SUCCESSFUL, 0,0)


    //    logTransitionEvent(trans,this)
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
        startPeriodicSync(this)
        startAudioDetection()
        startLocationUpdates()


        if (intent?.getBooleanExtra("run_sync", false) == true) {
            aggregateAppUsageData()
            syncAppUsageDataToRoomDB()
        }

        Log.d("TrackingService", "onStartCommand called.")

        // Check if the service is restarted
        if (intent == null) {
            Log.d("TrackingService", "Service restarted by AlarmManager.")
            showRestartNotification()
        }


        return START_STICKY
        //devend
    }


    private fun showRestartNotification() {
        val channelId = "service_restart_channel"
        val notificationManager = getSystemService(NotificationManager::class.java)

        // Create notification channel (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Service Restart",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for service restarts"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create a PendingIntent to open the app when the notification is clicked
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Replace with your app's notification icon
            .setContentTitle("Service Restarted")
            .setContentText("The Tracking Service has been restarted.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Show the notification
        notificationManager.notify(2001, notification)
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
                        screenOnEndTime=  System.currentTimeMillis()
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
    private fun syncAppUsageDataToRoomDB() {
//        Log.d("TrackingService", "syncAppUsageDataToFirebase: Starting data sync")
//        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
//            Log.w("TrackingService", "syncAppUsageDataToFirebase: User ID is null, aborting sync")
//            return
//        }
//        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
//        val userRef = firebaseDatabase.child("users").child(userId).child("phone_usage").child(date)
//
//        Log.d("TrackingService", "syncAppUsageDataToFirebase: Syncing screen time: $screenOnTime ms")
//        userRef.child("screen_time").setValue(screenOnTime)
//
//
//        dailyAppUsage.forEach { (packageName, appData) ->
//            val simplifiedAppName = simplifyAppName(packageName)
//            Log.d(
//                "TrackingService", "syncAppUsageDataToFirebase: App $simplifiedAppName - " +
//                        "Opened: ${appData.openCount} times, Duration: ${appData.totalDuration} ms"
//            )
//            val appRef = userRef.child("apps").child(simplifiedAppName)
//            appRef.child("opened").setValue(appData.openCount)
//            appRef.child("aggregated_duration").setValue(appData.totalDuration)
//        }
//
//        Log.d("TrackingService", "syncAppUsageDataToFirebase: Data sync complete for user $userId")

        if(!(toggleStates["App Sync"]?:false)){
            //     Toast.makeText(context,"Receiver ${activityType} , ${toggleStates[activityType].toString()}",
            //     Toast.LENGTH_SHORT).show();
            return
        }
        val usageMap: Map<String, List<Long>> = dailyAppUsage.mapValues { (_, appData) ->
            listOf(appData.openCount.toLong(), appData.totalDuration)
        }
        val db= AppUsageDB.getInstance(this)
        val dao=db.appusagelogDao()


        val log= AppUsageLog(
            usageMap=usageMap,
            screenOnTime = screenOnTime,
            screenStartTime = screenOnStartTime,
            screenEndTime = screenOnEndTime,
             date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )
        Log.d("TrackingService", "syncAppUsageDataToRoomdb: Log: $log")
        CoroutineScope(Dispatchers.IO).launch {
            dao.insert(log)
        }


    }


    private fun scheduleDailySyncAt11PM() {
        Log.d("TrackingService", "scheduleDailySyncAt11PM: Setting up daily sync alarm")
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, SyncReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 16)
            set(Calendar.MINUTE, 14)
            set(Calendar.SECOND, 50 )
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

    private fun startPeriodicSync(context:Context) {
        handler.postDelayed({ syncActivityData(currentActivity,context) }, syncInterval)
    }

    private fun syncActivityData(activityType: String,context:Context) {
//        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
//        firebaseDatabase.child("users").child(userId).child("activity_transitions").push().setValue(
//            mapOf(
//                "activity" to activityType,
//                "timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
//            )
//        )

        val db = AppDatabase.getInstance(context)
        val dao = db.activityLogDao()
        val log = ActivityLog(
            activityType = activityType,

            timestamp = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        )
        CoroutineScope(Dispatchers.IO).launch {
            dao.insert(log)
        }
    }

    private fun scheduleDailyNotifications() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Check if the app can schedule exact alarms
        if (!alarmManager.canScheduleExactAlarms()) {
            // Direct user to settings to enable exact alarms
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            startActivity(intent)
            return
        }

        // Schedule alarms
        scheduleAlarm(alarmManager, 19, 0, 0, 1001) // First alarm at 7:00 PM
        scheduleAlarm(alarmManager, 14, 12, 0, 1002) // Second alarm at 8:00 AM
    }

    private fun scheduleAlarm(
        alarmManager: AlarmManager,
        hour: Int,
        minute: Int,
        second: Int,
        requestCode: Int
    ) {
        try {
            val intent = Intent(this, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, second)

                // Ensure the alarm is set for the future
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )

            Log.d("Alarm", "Alarm set for: ${calendar.time}")

        } catch (e: SecurityException) {
            Log.e("Alarm", "SecurityException: ${e.message}")
            // Notify the user or log the issue
        }
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

    //useThis::
    private fun startActivityTransitionUpdates(context: Context) {
        val transitions = listOf(
            DetectedActivity.IN_VEHICLE, DetectedActivity.WALKING, DetectedActivity.RUNNING
        ).flatMap { activity ->
            listOf(
                ActivityTransition.Builder().setActivityType(activity).setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build(),
                ActivityTransition.Builder().setActivityType(activity).setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build()
            )
        }
        val pendingIntent = PendingIntent.getBroadcast(context, 0, Intent(context, TransitionReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
            ActivityRecognition.getClient(context).requestActivityTransitionUpdates(ActivityTransitionRequest(transitions), pendingIntent)
        }
    }

//    fun startActivityTransitionUpdates(context: Context) {
//        val transitions = listOf(
//            ActivityTransition.Builder().setActivityType(DetectedActivity.IN_VEHICLE)
//                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build(),
//            ActivityTransition.Builder().setActivityType(DetectedActivity.IN_VEHICLE)
//                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build(),
//            ActivityTransition.Builder().setActivityType(DetectedActivity.WALKING)
//                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build(),
//            ActivityTransition.Builder().setActivityType(DetectedActivity.WALKING)
//                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build(),
//            ActivityTransition.Builder().setActivityType(DetectedActivity.RUNNING)
//                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build(),
//            ActivityTransition.Builder().setActivityType(DetectedActivity.RUNNING)
//                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build()
//        )
//
//        val request = ActivityTransitionRequest(transitions)
//        val intent = Intent(context, TransitionReceiver::class.java)
//        val pendingIntent = PendingIntent.getBroadcast(
//            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
//        )
//
//        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED) {
//            val task = ActivityRecognition.getClient(context).requestActivityTransitionUpdates(request, pendingIntent)
//            task.addOnSuccessListener {
//                Log.d("ActivityTransition2", "Activity transitions successfully registered.")
//            }
//            task.addOnFailureListener {
//                Log.e("ActivityTransition2", "Failed to register activity transitions: ${it.message}")
//            }
//        }
//    }



    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        audioRecord?.apply { stop(); release() }
        isRecording = false
        handler.removeCallbacksAndMessages(null)
        unregisterScreenReceiver()

        Log.d("TrackingService", "Service destroyed. Attempting to restart...")


        // Restart logic
        val restartIntent = Intent(this, TrackingService::class.java)
        val pendingIntent = PendingIntent.getService(
            this,
            1,
            restartIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Schedule restart after 5 seconds
        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 5000, // 5-second delay
            pendingIntent
        )


    }

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val CONVERSATION_THRESHOLD = 2000
        var isSurveyReceived=false
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

            // Create an Intent to open the activity with the composable
            val notificationIntent = Intent(it, MainActivity::class.java)
            notificationIntent.putExtra("Survey","")
            notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

            // Wrap the Intent in a PendingIntent
            val pendingIntent = PendingIntent.getActivity(
                it,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Create the notification
            val notification = NotificationCompat.Builder(it, channelId)
                .setSmallIcon(R.drawable.ic_notification) // Replace with your app's notification icon
                .setContentTitle("Time for Your Daily Test")
                .setContentText("Please complete your daily health test.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            // Show the notification
            notificationManager.notify(1001, notification)
        }
    }
}


class RebootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                showRebootNotification(context, isQuickBoot = intent.action != Intent.ACTION_BOOT_COMPLETED)
            }
        }
    }

    private fun showRebootNotification(context: Context, isQuickBoot: Boolean) {
        val channelId = "reboot_notification_channel"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Reboot Notification",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for reboot events"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Create an Intent to open the app
        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Customize notification based on boot type
        val title = if (isQuickBoot) "Quick Boot Completed" else "Device Restarted"
        val message = if (isQuickBoot)
            "Device quick boot completed. Tap to resume your activities."
        else
            "Please tap to reopen the app and resume activity tracking."

        // Build the notification
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .apply {
                if (isQuickBoot) {
                    // Add a distinct style for quick boot notifications
                    setStyle(NotificationCompat.BigTextStyle()
                        .bigText(message)
                        .setSummaryText("Quick Boot"))
                }
            }
            .build()

        // Show the notification with different IDs for different boot types
        val notificationId = if (isQuickBoot) 3002 else 3001
        notificationManager.notify(notificationId, notification)
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


    class TransitionReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ActivityTransitionResult.hasResult(intent)) {
                val result = ActivityTransitionResult.extractResult(intent)
                result?.transitionEvents?.forEach { event ->
                    updateCurrentActivity(event)
                }
            }
        }

        private fun updateCurrentActivity(event: ActivityTransitionEvent) {
            currentActivity = when (event.activityType) {
                DetectedActivity.IN_VEHICLE -> "In Vehicle"
                DetectedActivity.WALKING -> "Walking"
                DetectedActivity.RUNNING -> "Running"
                else -> "Still"
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
            timestamp = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
        )
        CoroutineScope(Dispatchers.IO).launch {
            dao.insert(log)
        }
        Log.d("@@@@","worked3")
    }
}

}