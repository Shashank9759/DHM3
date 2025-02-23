package com.example.dhm30.Data.Database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.dhm30.Data.Entities.AudioLog
import com.example.dhm30.Data.DAOs.AudioLogDao


@Database(entities = [AudioLog::class], version = 1)
abstract class AudioDB : RoomDatabase() {
    abstract fun audiologDao(): AudioLogDao

    companion object {
        @Volatile
        private var INSTANCE: AudioDB? = null

        fun getInstance(context: Context): AudioDB {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AudioDB::class.java,
                    "audio_log_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
