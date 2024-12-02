package com.example.dhm20.Data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = [LocationLog::class], version = 1)
abstract class LocationDB : RoomDatabase() {
    abstract fun locationlogDao(): LocationLogDao

    companion object {
        @Volatile
        private var INSTANCE: LocationDB? = null

        fun getInstance(context: Context): LocationDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocationDB::class.java,
                    "location_log_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
