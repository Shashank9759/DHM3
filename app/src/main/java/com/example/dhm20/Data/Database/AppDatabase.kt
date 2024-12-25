package com.example.dhm20.Data.Database


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.dhm20.Data.Entities.ActivityLog
import com.example.dhm20.Data.DAOs.ActivityLogDao

@Database(entities = [ActivityLog::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun activityLogDao(): ActivityLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "activity_log_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
