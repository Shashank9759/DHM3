package com.example.dhm20.Data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


@Dao
interface AudioLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(log: AudioLog)

    @Query("SELECT * FROM audio_logs")
    fun getAllLogs(): List<AudioLog>

    @Delete
    fun delete(log: AudioLog)
}


