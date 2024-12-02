package com.example.dhm20.Data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query



@Dao
interface LocationLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(log: LocationLog)

    @Query("SELECT * FROM location_logs")
    fun getAllLogs(): List<LocationLog>

    @Delete
    fun delete(log: LocationLog)
}