package com.example.dhm30.Data.Database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.dhm30.Data.DAOs.ActivityLogDao
import com.example.dhm30.Data.DAOs.SurveyLogDao
import com.example.dhm30.Data.Entities.ActivityLog
import com.example.dhm30.Data.Entities.SurveyLog
import com.example.dhm30.Utils.UsageMapConverter

@Database(entities = [SurveyLog::class], version = 1)
@TypeConverters(UsageMapConverter::class)
abstract class SurveyDb : RoomDatabase() {
    abstract fun surveyLogDao(): SurveyLogDao

    companion object {
        @Volatile
        private var INSTANCE: SurveyDb? = null

        fun getInstance(context: Context): SurveyDb {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SurveyDb::class.java,
                    "survey_log_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
