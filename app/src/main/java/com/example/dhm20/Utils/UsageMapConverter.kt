package com.example.dhm20.Utils

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class UsageMapConverter {

    @TypeConverter
    fun fromUsageMap(value: Map<String, List<Long>>?): String? {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toUsageMap(value: String?): Map<String, List<Long>>? {
        val mapType = object : TypeToken<Map<String, List<Long>>>() {}.type
        return Gson().fromJson(value, mapType)
    }
}
