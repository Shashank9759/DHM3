package com.example.dhm20.Data.Entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "activity_appusage")
public data class AppUsageLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "usageMap") val usageMap: Map<String, List<Long>>,
    @ColumnInfo(name = "screenOnTime") val screenOnTime: Long,
    @ColumnInfo(name = "date")val date: String
)
