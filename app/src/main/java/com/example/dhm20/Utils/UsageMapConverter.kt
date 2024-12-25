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

    // TypeConverter for Map<Int, String>
    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>? {
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return Gson().fromJson(value, mapType)
    }

    // TypeConverter for Map<Int, Int>
    @TypeConverter
    fun fromStringIntMap(value: Map<String, Int>?): String? {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringIntMap(value: String?): Map<String, Int>? {
        val mapType = object : TypeToken<Map<String, Int>>() {}.type
        return Gson().fromJson(value, mapType)
    }
}
