package com.example.dhm30.Data.DAOs

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dhm30.Data.Entities.ActivityLog
import com.example.dhm30.Data.Entities.SurveyLog

@Dao
interface SurveyLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(log: SurveyLog)

    @Query("SELECT * FROM survey_logs")
    fun getAllLogs(): List<SurveyLog>

    @Delete
    fun delete(log: SurveyLog)
}