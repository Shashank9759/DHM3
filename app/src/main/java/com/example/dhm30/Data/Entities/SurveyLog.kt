package com.example.dhm30.Data.Entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "survey_logs")
public data class SurveyLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "answersMap") val answersMap: Map<String,String>,
    @ColumnInfo(name = "ratingsMap") val ratingsMap: Map<String,Int>,
    @ColumnInfo(name = "timestamp")val timestamp: String,
    @ColumnInfo(name = "finalscore")val finalscore: Int
)
