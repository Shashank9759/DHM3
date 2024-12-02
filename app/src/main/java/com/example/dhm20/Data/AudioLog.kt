package com.example.dhm20.Data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "audio_logs")
public data class AudioLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "conversation") val conversation: Int,
    @ColumnInfo(name = "timestamp") val timestamp: String,

)
