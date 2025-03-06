package com.example.dhm30


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
import com.example.dhm30.Data.Entities.ActivityLog
import com.example.dhm30.Data.Database.AppDatabase
import com.example.dhm30.Presentation.toggleStates
//import com.example.dhm30.TransitionReceiver
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
import android.net.ConnectivityManager
import android.net.Network
import com.example.dhm30.Data.Entities.AppUsageLog
import com.example.dhm30.Data.Database.AudioDB
import com.example.dhm30.Data.Entities.AudioLog
import com.example.dhm30.Data.Database.LocationDB
import com.google.android.gms.location.*
import com.google.android.gms.location.FusedLocationProviderClient
import android.provider.Settings
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager

import java.util.*
import com.example.dhm30.Data.Entities.LocationLog
import kotlinx.coroutines.tasks.await
import com.example.dhm30.Data.Database.AppUsageDB
import com.example.dhm30.Data.SyncWorker
import com.example.dhm30.Data.SyncWorker.Companion
import com.example.dhm30.Data.SyncWorker.Companion.getIdTokenFromPrefs
import com.example.dhm30.Helpers.isNetworkAvailable

data class AppUsageData(var openCount: Int = 0, var totalDuration: Long = 0)

private var currentActivity = "Still"
private val firebaseDatabase = FirebaseDatabase.getInstance().reference
private var isSyncingLocalData = false
// Connectivity manager to monitor network changes
private lateinit var connectivityManager: ConnectivityManager
private lateinit var connectivityCallback: ConnectivityManager.NetworkCallback



class TrackingService() : Service() {
    private lateinit var appContext: Context // Declare without initializing immediately
    private lateinit var appusagedb: AppUsageDB

    lateinit var notification: Notification

    private lateinit var usageStatsManager: UsageStatsManager
    private val dailyAppUsage = mutableMapOf<String, AppUsageData>()
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var screenOnTime = 0L
    private var screenOnStartTime = 0L
    private val screenReceiver = ScreenReceiver()
    private var currentAudioState = 0
    private var currentLocation: Pair<Double, Double>? = null
    private val audioLocationSyncInterval = 1000L // 1 second
    private val activitySyncInterval = 5000L // 5 seconds
    private val syncInterval = 5000L

    //devend

    val notificationId = 404
    val handler = Handler(Looper.getMainLooper())
    var count = 0


    //onCreate
    override fun onCreate() {
        super.onCreate()

        appContext = this.applicationContext // Proper initialization
        appusagedb = AppUsageDB.getInstance(appContext)

        startForegroundServiceWithNotification()


        initializeConnectivityListener()


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

    private fun initializeConnectivityListener() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        connectivityCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d("TrackingService", "Network available")
                syncLocalDataToFirebase(this@TrackingService)
            }

            override fun onLost(network: Network) {
                Log.d("TrackingService", "Network lost")
                notifyUser(
                    "Network Disconnected",
                    "Data will be saved locally until connection is restored."
                )
            }
        }

        connectivityManager.registerDefaultNetworkCallback(connectivityCallback)
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


    // Add this to the TrackingService class
    private fun isSurveyCompleted(context: Context): Boolean {
        val sharedPref = context.getSharedPreferences("survey_prefs", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("survey_completed", false)
    }

    private fun setSurveyCompleted(context: Context, completed: Boolean) {
        val sharedPref = context.getSharedPreferences("survey_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("survey_completed", completed)
            apply()
        }
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

        // Create a new intent to open the main app screen (not survey)
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // Ensure a unique request code so it doesn’t get overridden
        val pendingIntent = PendingIntent.getActivity(
            this,
            1000, // Unique request code for foreground notification
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Monitoring Activity")
            .setContentText("Collecting activity transition data")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(pendingIntent) // Ensuring it always opens the app
            .build()

        Log.d("TrackingService", "Attempting to start foreground")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
            startForeground(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                        or ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            )
        } else {
            startForeground(notificationId, notification)
        }

        Log.d("TrackingService", "Foreground started")
        Toast.makeText(this, "Foreground service started", Toast.LENGTH_SHORT).show()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("onstartcommenad", "working")
        val trans = ActivityTransitionEvent(DetectedActivity.RUNNING, ACTIVITY_TRANSITION_ENTER, 0L)
        val sleepEvent = SleepSegmentEvent(
            1625000000000L,
            1625030000000L,
            SleepSegmentEvent.STATUS_SUCCESSFUL,
            0,
            0
        )


        startActivityTransitionUpdates(this)
        startPeriodicSync()
        startAudioDetection()
        startLocationUpdates()
        startLocationSync()

        if (intent?.getBooleanExtra("run_sync", false) == true) {
            aggregateAppUsageData()
//            syncAppUsageDataToRoomDB()
        }

        handler.postDelayed({
            checkDataCollectionStatus()
            handler.postDelayed({ checkDataCollectionStatus() }, 3 * 60 * 60 * 1000) // Check every 3 hours
        }, 3 * 60 * 60 * 1000)
//        if (intent?.getBooleanExtra("run_sync", false) == true) {
//            Log.d("TrackingService", "Running daily sync at 11 PM")
//            CoroutineScope(Dispatchers.IO).launch {
//                aggregateAppUsageData()
//                if (Helpers.isNetworkAvailable(applicationContext)) {
//                    syncAppUsageData()
//                } else {
//                    Log.d("TrackingService", "Network unavailable. Data saved locally.")
//                }
//            }
//        }


        // Check if the service is restarted
        if (intent == null) {
            Log.d("TrackingService", "Service restarted by AlarmManager.")

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
                        Log.d(
                            "ScreenReceiver",
                            "onReceive: Screen OFF. Accumulated screen time: $screenOnTime ms"
                        )
                    }
                }
            }
        }
    }


    private fun aggregateAppUsageData() {
        Log.d("TrackingService", "Aggregating app usage data")
        val startTime = System.currentTimeMillis() - AlarmManager.INTERVAL_DAY
        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, System.currentTimeMillis()
        )

        val aggregatedData = mutableMapOf<String, List<Long>>()

        usageStatsList.forEach { usageStat ->
            if (usageStat.totalTimeInForeground > 0) {
                val appData = aggregatedData.getOrPut(usageStat.packageName) { listOf(0L, 0L) }
                aggregatedData[usageStat.packageName] = listOf(
                    appData[0] + 1,
                    appData[1] + usageStat.totalTimeInForeground
                )
            }
        }

        val log = AppUsageLog(
            usageMap = aggregatedData,
            screenOnTime = screenOnTime,
            screenStartTime = screenOnStartTime,
            screenEndTime = System.currentTimeMillis(),
            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )

        if (isNetworkAvailable(this)) {
            syncAppUsageDataToFirebase(listOf(log)) // Sync immediately if internet is available
            Log.d("Sync","syncing")
        } else {
            saveAppUsageToLocalDb(log) // Save locally if the internet is unavailable
        }
    }

    private fun saveAppUsageToLocalDb(log: AppUsageLog) {
        val db = AppUsageDB.getInstance(this)
        CoroutineScope(Dispatchers.IO).launch {
            db.appusagelogDao().insert(log)
            Log.d("TrackingService", "App usage data saved locally")
        }
    }

//


    private fun syncAppUsageDataToFirebase(log: List<AppUsageLog>) {
        val db = AppUsageDB.getInstance(this)
        CoroutineScope(Dispatchers.IO).launch {
            val logs = log
            logs.forEach { log ->
                val userId = getIdTokenFromPrefs(this@TrackingService) ?: return@forEach
                val userRef = firebaseDatabase.child("users")
                    .child(userId)
                    .child("phone_usage")
                    .child(log.date)

                userRef.child("screen_time").setValue(log.screenOnTime)
                log.usageMap.forEach { (packageName, appData) ->
                    val simplifiedAppName = simplifyAppName(packageName)
                    val appRef = userRef.child("apps").child(simplifiedAppName)
                    appRef.child("opened").setValue(appData[0])
                    appRef.child("aggregated_duration").setValue(appData[1])
                }

                db.appusagelogDao().delete(log)
                Log.d("TrackingService", "Synced and deleted local log: $log")
            }
        }
    }

    fun getIdTokenFromPrefs(context: Context): String? {
        val sharedPref = context.getSharedPreferences("My_App_Prefs", Context.MODE_PRIVATE)
        val email = sharedPref.getString("user_email", null)
        val encodedEmail = email?.replace(".", "_")?.replace("@", "_")

        return encodedEmail
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

//    private suspend fun syncAppUsageData() {
//        val appUsageDao = appusagedb.appusagelogDao()
//        val logs = appUsageDao.getAllLogs()
//
//        logs.forEach { log ->
//            val userId = getIdTokenFromPrefs(appContext) ?: "anonymous"
//            val userRef = firebaseDatabase.child("users")
//                .child(userId)
//                .child("phone_usage")
//                .child(log.date)
//
//            log.usageMap.forEach { (packageName, appData) ->
//                val simplifiedAppName = simplifyAppName(packageName)
//                val appRef = userRef.child("apps").child(simplifiedAppName)
//                appRef.child("opened").setValue(appData[0])
//                appRef.child("aggregated_duration").setValue(appData[1])
//            }
//            userRef.child("screen_time").setValue(log.screenOnTime).await()
//
//            // Remove log after successful sync
//            appUsageDao.delete(log)
//            Log.d("SyncWorker", "App usage log synced and deleted locally: $log")
//        }
//    }


    private fun scheduleDailySyncAt11PM() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, SyncReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY,23)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
        Log.d("TrackingService", "Daily sync alarm scheduled for ${calendar.time}")
    }


    private fun startPeriodicSync() {
        handler.postDelayed({
            if (isInternetAvailable()) {
                syncActivityData(currentActivity)
            } else {
                saveDataToLocalDb(currentActivity)
            }
            startPeriodicSync()
        }, activitySyncInterval)
    }

    private fun startPeriodicAudioSync() {
        handler.postDelayed({
            if (isInternetAvailable()) {
                startAudioDetection()
//                syncAudioData(currentAudioState)
            } else {
                startAudioDetection()
//                saveAudioToLocalDb(currentAudioState)
            }
            startPeriodicAudioSync()
        }, audioLocationSyncInterval)
    }

    private fun startLocationSync(){
        handler.postDelayed({
            if (Helpers.isNetworkAvailable(this)) {
                syncLocalLocationDataToFirebase() // Sync locally stored data first
            }
            currentLocation?.let {
                if (Helpers.isNetworkAvailable(this)) {
                    syncLocationData(it.first, it.second) // Real-time sync
                } else {
                    saveLocationToLocalDb(it.first, it.second) // Save locally if offline
                }
            }
            startLocationSync() // Schedule the next sync
        }, audioLocationSyncInterval)
    }

    private fun syncActivityData(activityType: String) {
        val userId =  getIdTokenFromPrefs(this@TrackingService) ?: return
        firebaseDatabase.child("users").child(userId).child("activity_transitions").push()
            .setValue(
                mapOf(
                    "activity" to activityType,
                    "timestamp" to SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                    ).format(Date())
                )
            )
            .addOnSuccessListener {
                Log.d("TrackingService", "Activity data synced successfully")
                saveLastDataCollectionTime(this@TrackingService) // Save last data collection time
            }
            .addOnFailureListener {
                Log.e("TrackingService", "Failed to sync activity data: ${it.message}")
                saveDataToLocalDb(activityType)
            }
    }

    private fun syncLocalDataToFirebase(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            // Sync activity logs
            val activityDao = AppDatabase.getInstance(context).activityLogDao()
            val activityLogs = activityDao.getAllLogs()
            if (activityLogs.isNotEmpty()) {
                activityLogs.forEach { log ->
                    try {
                        val userId =  SyncWorker.getIdTokenFromPrefs(this@TrackingService) ?: return@forEach
                        firebaseDatabase.child("users")
                            .child(userId)
                            .child("activity_transitions")
                            .push()
                            .setValue(
                                mapOf(
                                    "activity" to log.activityType,
                                    "timestamp" to log.timestamp
                                )
                            ).await() // Wait for Firebase operation to complete
                        activityDao.delete(log) // Remove the log after successful sync
                    } catch (e: Exception) {
                        Log.e(
                            "TrackingService",
                            "Failed to sync activity log: ${e.message}"
                        )
                    }
                }
            }

            // Sync audio logs
            val audioDao = AudioDB.getInstance(context).audiologDao()
            val audioLogs = audioDao.getAllLogs()
            if (audioLogs.isNotEmpty()) {
                audioLogs.forEach { log ->
                    try {
                        val userId =  getIdTokenFromPrefs(this@TrackingService) ?: return@forEach
                        firebaseDatabase.child("users")
                            .child(userId)
                            .child("audio_data")
                            .push()
                            .setValue(
                                mapOf(
                                    "conversation" to log.conversation,
                                    "timestamp" to log.timestamp
                                )
                            ).await()
                        audioDao.delete(log) // Remove the log after successful sync
                        Log.e("TrackingService", "sync audio log offline")
                    } catch (e: Exception) {
                        Log.e("TrackingService", "Failed to sync audio log: ${e.message}")
                    }
                }
            }
        }
    }
    private fun syncLocalLocationDataToFirebase() {
        CoroutineScope(Dispatchers.IO).launch {
            val locationDao = LocationDB.getInstance(this@TrackingService).locationlogDao()
            var offset = 0
            val batchSize = 100 // Adjust batch size as needed
            while (true) {
                val logs = locationDao.getLogsPaginated(batchSize, offset)
                if (logs.isEmpty()) break // Exit loop if no more logs

                logs.forEach { log ->
                    try {
                        val userId =  SyncWorker.getIdTokenFromPrefs(this@TrackingService) ?: return@forEach
                        firebaseDatabase.child("users")
                            .child(userId)
                            .child("gps_data")
                            .push()
                            .setValue(
                                mapOf(
                                    "latitude" to log.latitude,
                                    "longitude" to log.longitude,
                                    "timestamp" to log.timestamp
                                )
                            ).await()
                        locationDao.delete(log) // Remove the log after successful sync
                    } catch (e: Exception) {
                        Log.e("TrackingService", "Failed to sync location log: ${e.message}")
                    }
                }
                offset += batchSize
            }
            Log.d("TrackingService", "Local location data synced successfully.")
        }
    }


        private fun saveDataToLocalDb(activityType: String) {
        val db = AppDatabase.getInstance(this)
        val dao = db.activityLogDao()
        val log = ActivityLog(
            activityType = activityType,
            timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )
        CoroutineScope(Dispatchers.IO).launch {
            dao.insert(log)
            Log.d("TrackingService", "Activity data saved locally")
        }
    }

    private fun isInternetAvailable(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork
        return activeNetwork != null
    }

    private fun notifyUser(title: String, message: String,isDailyTest: Boolean = false) {
        val channelId = "tracking_service_channel"
        val notificationManager = getSystemService(NotificationManager::class.java)

        val notificationIntent = if (!isDailyTest) {
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        } else {
            null // Daily test notifications will have their own logic
        }

        val pendingIntent = notificationIntent?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }

    private fun scheduleDailyNotifications() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        Log.d("TrackingService", "onCreate: Starting service2")
        // Check if the app can schedule exact alarms
        if (!alarmManager.canScheduleExactAlarms()) {
            // Direct user to settings to enable exact alarms
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            startActivity(intent)
            return
        }

        // Schedule alarms


        scheduleAlarm(alarmManager, 21, 0, 0, 1002) // Second alarm at 9:00 PM
      //  scheduleAlarm(alarmManager, 14, 47, 0, 1003) // Second alarm at 9:00 PM
//        scheduleAlarm(alarmManager, 12, 57, 0, 1004) // Second alarm at 9:00 PM
//        scheduleAlarm(alarmManager, 12, 55, 0, 1005) // Second alarm at 9:00 PM
//        scheduleAlarm(alarmManager, 12, 53, 0, 1005) // Second alarm at 9:00 PM



    }

    private fun scheduleAlarm(
        alarmManager: AlarmManager,
        hour: Int,
        minute: Int,
        second: Int,
        requestCode: Int
    ) {
        try {
            Log.d("TrackingService", "onCreate: Starting service3")
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
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("AudioDetection", "Permission not granted for RECORD_AUDIO")
            return
        }

        val bufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e("AudioDetection", "Invalid buffer size: $bufferSize")
            return
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e("AudioDetection", "AudioRecord initialization failed")
            return
        }

        Log.d("AudioDetection", "Microphone is now active and listening")
        audioRecord?.startRecording()
        isRecording = true
        Thread { continuouslyMonitorAudio(bufferSize) }.start()
    }


    private fun continuouslyMonitorAudio(bufferSize: Int) {
        val audioBuffer = ShortArray(bufferSize)

        while (isRecording) {
            val readSize = audioRecord?.read(audioBuffer, 0, bufferSize) ?: -1

            if (readSize < 0) {
                Log.e("AudioDetection", "Error reading audio data: $readSize")
                continue
            }

            if (readSize > 0) {
                val maxAmplitude = audioBuffer.maxOrNull()?.toInt() ?: 0
                Log.d("AudioDetection", "Microphone readSize: $readSize, Max Amplitude: $maxAmplitude")

                val isConversationDetected = maxAmplitude > CONVERSATION_THRESHOLD
                currentAudioState = if (isConversationDetected) 1 else 0

                Log.d(
                    "AudioDetection",
                    if (isConversationDetected) "Conversation detected" else "No conversation detected"
                )

                if (isInternetAvailable()) {
                    syncAudioData(currentAudioState)
                } else {
                    saveAudioToLocalDb(currentAudioState)
                }
            }

            Thread.sleep(1000) // Sampling interval
        }

        Log.d("AudioDetection", "Microphone is no longer actively listening")
    }


    private fun syncAudioData(isConversationDetected: Int) {
        val userId =  getIdTokenFromPrefs(this@TrackingService) ?: return
        firebaseDatabase.child("users").child(userId).child("audio_data").push()
            .setValue(
                mapOf(
                    "conversation_detected" to isConversationDetected,
                    "timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                )
            )
            .addOnSuccessListener {
                Log.d("TrackingService", "Audio data synced successfully: $isConversationDetected")
                saveLastDataCollectionTime(this@TrackingService) // Save last data collection time
            }
            .addOnFailureListener {
                Log.e("TrackingService", "Failed to sync audio data: ${it.message}")
                saveAudioToLocalDb(isConversationDetected)
            }
    }

    private fun saveAudioToLocalDb(isConversationDetected: Int) {
        val db = AudioDB.getInstance(this)
        val dao = db.audiologDao()
        val log = AudioLog(
            conversation = isConversationDetected,
            timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )
        CoroutineScope(Dispatchers.IO).launch {
            dao.insert(log)
            Log.d("TrackingService", "Audio data saved locally")
        }
    }

    private fun startLocationUpdates() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val locationRequest =
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 60000).build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.forEach { location ->
                    currentLocation = Pair(location.latitude, location.longitude)
                }
            }
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                mainLooper
            )
        }
    }
    private fun syncLocationData(latitude: Double, longitude: Double) {
        val userId =  getIdTokenFromPrefs(this@TrackingService) ?: return
        firebaseDatabase.child("users").child(userId).child("gps_data").push()
            .setValue(
                mapOf(
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "timestamp" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                )
            )
            .addOnSuccessListener {
                Log.d("TrackingService", "Location data synced successfully")
                saveLastDataCollectionTime(this@TrackingService) // Save last data collection time
            }
            .addOnFailureListener {
                Log.e("TrackingService", "Failed to sync location data: ${it.message}")
                saveLocationToLocalDb(latitude, longitude)
            }
    }

    private fun saveLocationToLocalDb(latitude: Double, longitude: Double) {
        val db = LocationDB.getInstance(this)
        val dao = db.locationlogDao()
        val log = LocationLog(
            latitude = latitude,
            longitude = longitude,
            timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )
        CoroutineScope(Dispatchers.IO).launch {
            dao.insert(log)
            Log.d("TrackingService", "Location data saved locally")
        }
    }


    //useThis::
    private fun startActivityTransitionUpdates(context: Context) {
        val transitions = listOf(
            DetectedActivity.IN_VEHICLE, DetectedActivity.WALKING, DetectedActivity.RUNNING
        ).flatMap { activity ->
            listOf(
                ActivityTransition.Builder().setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER).build(),
                ActivityTransition.Builder().setActivityType(activity)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT).build()
            )
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, Intent(context, TransitionReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            ActivityRecognition.getClient(context).requestActivityTransitionUpdates(
                ActivityTransitionRequest(transitions), pendingIntent
            )
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        audioRecord?.apply { stop(); release() }
        isRecording = false
        connectivityManager.unregisterNetworkCallback(connectivityCallback)
        handler.removeCallbacksAndMessages(null)
        unregisterScreenReceiver()

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
        private const val CONVERSATION_THRESHOLD = 2500
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    class SyncReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            context?.let {
                val serviceIntent = Intent(it, TrackingService::class.java)
                serviceIntent.putExtra("run_sync", true)
                it.startService(serviceIntent)
                Log.d("SyncReceiver", "Triggered daily sync at 11 PM")
            }
        }
    }

    class NetworkChangeReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (Helpers.isNetworkAvailable(context)) {
                Log.d("NetworkChangeReceiver", "Internet available. Syncing local data.")

                // Sync local app usage data to Firebase
                syncAppUsageData(context)


            }
        }

        private fun syncAppUsageData(context: Context) {
            val db = AppUsageDB.getInstance(context)
            val dao = db.appusagelogDao()
            CoroutineScope(Dispatchers.IO).launch {
                val logs = dao.getAllLogs()
                logs.forEach { log ->
                    val userId =  getIdTokenFromPrefs(context) ?: return@forEach
                    val userRef = FirebaseDatabase.getInstance().reference
                        .child("users")
                        .child(userId)
                        .child("phone_usage")
                        .child(log.date)

                    // Sync screen time
                    userRef.child("screen_time").setValue(log.screenOnTime)

                    // Sync app usage data
                    log.usageMap.forEach { (appName, appData) ->
                        val appRef = userRef.child("apps").child(appName)
                        appRef.child("opened").setValue(appData[0])
                        appRef.child("aggregated_duration").setValue(appData[1])
                    }

                    // Remove synced data from local DB
                    dao.delete(log)
                    Log.d("NetworkChangeReceiver", "Synced and deleted local app usage log: $log")
                }
            }
        }}

       class NotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            context?.let {
                val notificationManager =
                    it.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

                // Check if the survey was completed
                val sharedPref = it.getSharedPreferences("survey_prefs", Context.MODE_PRIVATE)
                val surveyCompleted = sharedPref.getBoolean("survey_completed", false)

                if (!surveyCompleted) {
                    // Create an Intent to open the activity with the composable
                    val notificationIntent = Intent(it, MainActivity::class.java)
                    notificationIntent.putExtra("Survey", "")
                    notificationIntent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                    // Wrap the Intent in a PendingIntent
                    val pendingIntent = PendingIntent.getActivity(
                        it,
                        2000,
                        notificationIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    // Create the notification
                    val notification = NotificationCompat.Builder(it, channelId)
                        .setSmallIcon(R.drawable.ic_notification) // Replace with your app's notification icon
                        .setContentTitle("Time for Your Daily Test")
                        .setContentText("Please complete your daily test.")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .build()

                    // Show the notification
                    notificationManager.notify(1001, notification)

                    // Reschedule the notification if the survey is not completed
                    val alarmManager = it.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val calendar = Calendar.getInstance().apply {
                        add(Calendar.HOUR_OF_DAY, 1) // Reschedule after 1 hour
                    }

                    val rescheduleIntent = Intent(it, NotificationReceiver::class.java)
                    val reschedulePendingIntent = PendingIntent.getBroadcast(
                        it,
                        1001,
                        rescheduleIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        reschedulePendingIntent
                    )
                }
            }
        }
    }



    class RebootReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
                context?.let {
                    val trackingServiceIntent = Intent(it, TrackingService::class.java)
                    it.startService(trackingServiceIntent)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.startForegroundService(trackingServiceIntent)
                    }

                    val alarmManager = it.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val syncIntent = Intent(it, SyncReceiver::class.java)
                    val pendingSyncIntent = PendingIntent.getBroadcast(
                        it, 0, syncIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + 5000,
                        pendingSyncIntent
                    )
                }
            }
        }
    }

    class TransitionReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (ActivityTransitionResult.hasResult(intent)) {
                val result = ActivityTransitionResult.extractResult(intent)
                result?.transitionEvents?.forEach { event ->
                    handleTransitionEvent(event, context)
                }
            }
        }

        private fun handleTransitionEvent(event: ActivityTransitionEvent, context: Context) {
            val activity = when (event.activityType) {
                DetectedActivity.IN_VEHICLE -> "In Vehicle"
                DetectedActivity.WALKING -> "Walking"
                DetectedActivity.RUNNING -> "Running"
                else -> "Still"
            }

            currentActivity = activity
        }
    }


    fun saveLastDataCollectionTime(context: Context) {
        val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putLong("last_data_collection_time", System.currentTimeMillis())
            apply()
        }
    }

    fun getLastDataCollectionTime(context: Context): Long {
        val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return sharedPref.getLong("last_data_collection_time", 0)
    }

    fun getInstallationTime(context: Context): Long {
        val sharedPref = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return sharedPref.getLong("installation_time", 0)
    }

    private fun checkDataCollectionStatus() {
        val installationTime = getInstallationTime(this)
        val lastDataCollectionTime = getLastDataCollectionTime(this)

        if (installationTime != 0L && lastDataCollectionTime == 0L) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - installationTime > 3 * 60 * 60 * 1000) { // 3 hours
                notifyUser(
                    "Resume Data Collection",
                    "Please enable data collection by opening the app and/or connecting to a network."
                )
            }
        } else if (lastDataCollectionTime != 0L) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastDataCollectionTime > 3 * 60 * 60 * 1000) { // 3 hours
                notifyUser(
                    "Resume Data Collection",
                    "Please resume data collection by opening the app and/or connecting to a network."
                )
            }
        }
    }


}

