package com.example.dhm20.Data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.dhm20.Data.Database.AppDatabase
import com.example.dhm20.Data.Database.AppUsageDB
import com.example.dhm20.Data.Database.AudioDB
import com.example.dhm20.Data.Database.LocationDB
import com.example.dhm20.Data.Database.SurveyDb
import com.example.dhm20.MainActivity
import com.example.dhm20.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SyncWorker(val appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    private val firebaseDatabase = FirebaseDatabase.getInstance().reference
    private val db = AppDatabase.getInstance(applicationContext)
    private val audiodb = AudioDB.getInstance(applicationContext)
    private val locationdb = LocationDB.getInstance(applicationContext)
    private val appusagedb = AppUsageDB.getInstance(applicationContext)
    private val surveydb = SurveyDb.getInstance(applicationContext)

    override suspend fun doWork(): Result {
        val logs = db.activityLogDao().getAllLogs()
        val audioLogs = audiodb.audiologDao().getAllLogs()
        val locationLogs = locationdb.locationlogDao().getAllLogs()
        val AppUsageLogs = appusagedb.appusagelogDao().getAllLogs()
        val SurveyLogs = surveydb.surveyLogDao().getAllLogs()

        if((logs==null || logs!!.isEmpty())
            && (audioLogs==null || audioLogs!!.isEmpty())
            && (locationLogs==null || locationLogs!!.isEmpty())
            && (AppUsageLogs==null || AppUsageLogs!!.isEmpty())
            && (SurveyLogs==null || SurveyLogs!!.isEmpty())

        ) {
            return Result.failure()

        }
        else{
            if (logs!=null && logs!!.isNotEmpty()) {
                Log.d("log@@@@", "log is not null")
                // Iterate over each log and upload to Firebase
                for (log in logs) {
                    val userId = getIdTokenFromPrefs(appContext) ?: "anonymous"

                    try {
                        // Upload log to Firebase
                        firebaseDatabase.child("users")
                            .child(userId)
                            .child("activity_transitions")
                            .push()
                            .setValue(log)
                            .addOnSuccessListener {
                                Log.d("SyncWorker", "Successfully synced log: $log")

                                // Run delete operation after successful sync in a coroutine context (background thread)
                                // This ensures we are running database operations in the correct context
                                CoroutineScope(Dispatchers.IO).launch {
                                    db.activityLogDao().delete(log)
                                    Log.d("SyncWorker", "Deleted log: $log from Room DB")
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("SyncWorker", "Error syncing log: ${e.message}")
                            }
                    } catch (e: Exception) {
                        Log.e("SyncWorker", "Error syncing log: ${e.message}")
                        // return Result.failure()  // Failure on syncing any log
                    }
                }

            }
            if (audioLogs!=null && audioLogs!!.isNotEmpty()) {
                Log.d("audiolog@@@@", "log is not null")
                // Iterate over each log and upload to Firebase
                for (log in audioLogs) {
                    val userId = getIdTokenFromPrefs(appContext) ?: "anonymous"

                    try {
                        // Upload log to Firebase
                        firebaseDatabase.child("users")
                            .child(userId)
                            .child("audio_data")
                            .push()
                            .setValue(log)
                            .addOnSuccessListener {
                                Log.d("SyncWorker", "Successfully Audio synced log: $log")

                                // Run delete operation after successful sync in a coroutine context (background thread)
                                // This ensures we are running database operations in the correct context
                                CoroutineScope(Dispatchers.IO).launch {
                                    audiodb.audiologDao().delete(log)
                                    Log.d("SyncWorker", "Deleted Audio log: $log from Room DB")
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("SyncWorker", "Error Audio syncing log: ${e.message}")
                            }
                    } catch (e: Exception) {
                        Log.e("SyncWorker", "Error Audio syncing log: ${e.message}")
                        //  return Result.failure()  // Failure on syncing any log
                    }
                }

            }

            if (locationLogs!=null && locationLogs!!.isNotEmpty()) {
                Log.d("locationlog@@@@", "log is not null")
                // Iterate over each log and upload to Firebase
                for (log in locationLogs) {
                    val userId =getIdTokenFromPrefs(appContext) ?: "anonymous"

                    try {
                        // Upload log to Firebase
                        firebaseDatabase.child("users")
                            .child(userId)
                            .child("gps_data")
                            .push()
                            .setValue(log)
                            .addOnSuccessListener {
                                Log.d("SyncWorker", "Successfully Location synced log: $log")

                                // Run delete operation after successful sync in a coroutine context (background thread)
                                // This ensures we are running database operations in the correct context
                                CoroutineScope(Dispatchers.IO).launch {
                                    locationdb.locationlogDao().delete(log)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("SyncWorker", "Error Audio syncing log: ${e.message}")
                            }
                    } catch (e: Exception) {
                        Log.e("SyncWorker", "Error Audio syncing log: ${e.message}")
                        //  return Result.failure()  // Failure on syncing any log
                    }
                }

            }
            if (AppUsageLogs!=null && AppUsageLogs!!.isNotEmpty()) {
                Log.d("appusagelog@@@@", "log is not null")
                // Iterate over each log and upload to Firebase
                for (log in AppUsageLogs) {
                    val userId = getIdTokenFromPrefs(appContext) ?: "anonymous"

                    try {
                        // Upload log to Firebase
                        val userRef = firebaseDatabase.child("users")
                            .child(userId)
                            .child("phone_usage")
                            .child(log.date)



                        log.usageMap.forEach { (packageName, appData) ->
                            val simplifiedAppName = simplifyAppName(packageName)
                            Log.d(
                                "TrackingService",
                                "syncAppUsageDataToFirebase: App $simplifiedAppName - " +
                                        "Opened: ${appData.get(0)} times, Duration: ${appData.get(1)} ms"
                            )
                            val appRef = userRef.child("apps").child(simplifiedAppName)
                            appRef.child("opened").setValue(appData.get(0))
                            appRef.child("aggregated_duration").setValue(appData.get(1))
                        }

                        userRef.child("screen_time").setValue(log.screenOnTime).addOnSuccessListener {
                            Log.d("SyncWorker", "Successfully appusage synced log: $log")
                            CoroutineScope(Dispatchers.IO).launch {
                                appusagedb.appusagelogDao().delete(log)
                            }
                        }


                    } catch (e: Exception) {
                        Log.e("SyncWorker", "Error appusage syncing log: ${e.message}")
                        //  return Result.failure()  // Failure on syncing any log
                    }
                }

          }


            if (SurveyLogs!=null && SurveyLogs!!.isNotEmpty()) {
                Log.d("surveylog@@@@", "log is not null")
                // Iterate over each log and upload to Firebase
                for (log in SurveyLogs) {
                    val userId = getIdTokenFromPrefs(appContext) ?: "anonymous"

                  try {
                     firebaseDatabase.child("users")
                          .child(userId)
                          .child("Survey")
                         .child(log.timestamp)
                          .setValue(log)
                          .addOnSuccessListener {
                              Log.d("SyncwokerSurvey","Succesfull")
                              CoroutineScope(Dispatchers.IO).launch {
                                  surveydb.surveyLogDao().delete(log)
                              }
                          }
                          .addOnFailureListener {

                                Log.d("SyncwokerSurvey","unSuccesfull  - ${it.message}")


                            }





                    } catch (e: Exception) {
                        Log.e("SyncWorker", "Error survey syncing log: ${e.message}")
                        //  return Result.failure()  // Failure on syncing any log
                    }
                }

            }


            return Result.success()

        }

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
    fun getIdTokenFromPrefs(context: Context): String? {
        val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("uid_token", null)
    }
}

