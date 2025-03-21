package com.example.dhm30.Data.DAOs


import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.dhm30.Data.Entities.ActivityLog

@Dao
interface ActivityLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(log: ActivityLog)

    @Query("SELECT * FROM activity_logs")
    fun getAllLogs(): List<ActivityLog>

    @Delete
    fun delete(log: ActivityLog)
}