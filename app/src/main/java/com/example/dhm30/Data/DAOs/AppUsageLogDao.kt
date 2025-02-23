package com.example.dhm30.Data.DAOs

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dhm30.Data.Entities.AppUsageLog


@Dao
interface AppUsageLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(log: AppUsageLog)

    @Query("SELECT * FROM activity_appusage")
    fun getAllLogs(): List<AppUsageLog>

    @Delete
    fun delete(log: AppUsageLog)
}