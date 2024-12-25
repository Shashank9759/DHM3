package com.example.dhm20

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.dhm20.Data.Entities.ActivityLog
import com.example.dhm20.Data.Database.AppDatabase
import com.example.dhm20.Presentation.toggleStates
import com.google.android.gms.location.SleepSegmentEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SleepReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (SleepSegmentEvent.hasEvents(intent)) {
            val events = SleepSegmentEvent.extractEvents(intent)
            events?.forEach { event ->
                Log.d("SleepReceiver", "Sleep event: ${event.startTimeMillis} - ${event.endTimeMillis}")
                Toast.makeText(
                    context,
                    "Sleep detected: ${event.startTimeMillis} to ${event.endTimeMillis}",
                    Toast.LENGTH_SHORT
                ).show()
                logSleepEvent(event,context)
            }
        } else {
            Log.d("SleepReceiver", "No sleep events detected.")
        }
    }

    companion object{




        fun logSleepEvent(event: SleepSegmentEvent, context: Context) {
            val db = AppDatabase.getInstance(context)
            val dao = db.activityLogDao()

            if(!(toggleStates["Sleep"]?:false)){
                if(!(toggleStates["Sleep"]?:false)){
                 //   Toast.makeText(context,"Receiver ${"Sleep"} , ${toggleStates["Sleep"].toString()}",
                  //      Toast.LENGTH_SHORT).show();
                    return
                }
                return

            }
            val log = ActivityLog(
                activityType = "Sleep",
                transitionType = "Sleep detected from ${event.startTimeMillis} to ${event.endTimeMillis}",
                timestamp = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(Date())
            )
            CoroutineScope(Dispatchers.IO).launch {
                dao.insert(log)
            }
            Log.d("SleepEvent", "Logged sleep event: $log")
        }

    }
}
