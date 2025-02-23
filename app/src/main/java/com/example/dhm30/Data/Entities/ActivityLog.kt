package com.example.dhm30.Data.Entities
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_logs")
public data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "activityType") val activityType: String,
    @ColumnInfo(name = "timestamp")val timestamp: String
)