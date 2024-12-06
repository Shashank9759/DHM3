package com.example.dhm20.Data.Database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.dhm20.Data.Entities.AppUsageLog
import com.example.dhm20.Data.DAOs.AppUsageLogDao
import com.example.dhm20.Utils.UsageMapConverter


@Database(entities = [AppUsageLog::class], version = 1)
@TypeConverters(UsageMapConverter::class)
abstract class AppUsageDB : RoomDatabase() {
    abstract fun appusagelogDao(): AppUsageLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppUsageDB? = null

        fun getInstance(context: Context): AppUsageDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppUsageDB::class.java,
                    "appusage_log_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
