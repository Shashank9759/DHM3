package com.example.dhm20.Data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SyncWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {
    private val firebaseDatabase = FirebaseDatabase.getInstance().reference
    private val db = AppDatabase.getInstance(applicationContext)

    override suspend fun doWork(): Result {
        val logs = db.activityLogDao().getAllLogs()


        if (logs!=null && logs!!.isNotEmpty()) {
            Log.d("log@@@@", "log is not null")
            // Iterate over each log and upload to Firebase
            for (log in logs) {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

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
                    return Result.failure()  // Failure on syncing any log
                }
            }
            return Result.success()
        } else {
            Log.d("log@@@@", "log is null")
            return Result.failure()  // No logs to sync
        }
    }
}
