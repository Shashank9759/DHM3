package com.example.dhm30.Data.Entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "activity_appusage")
public data class AppUsageLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "usageMap") val usageMap: Map<String, List<Long>>,
    @ColumnInfo(name = "screenOnTime") val screenOnTime: Long,
    @ColumnInfo(name = "screenStartTime") val screenStartTime: Long,
    @ColumnInfo(name = "screenEndTime") val screenEndTime: Long,
    @ColumnInfo(name = "date")val date: String
)
