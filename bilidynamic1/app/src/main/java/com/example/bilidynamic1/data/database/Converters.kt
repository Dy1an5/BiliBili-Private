package com.example.bilidynamic1.data.database

import androidx.room.TypeConverter
import com.example.bilidynamic1.data.model.Group
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromFilterType(value: Group.FilterType): String = value.name

    @TypeConverter
    fun toFilterType(value: String): Group.FilterType = Group.FilterType.valueOf(value)

    @TypeConverter
    fun fromSet(value: Set<Long>?): String = gson.toJson(value ?: emptySet<Long>())

    @TypeConverter
    fun toSet(value: String?): Set<Long> {
        if (value.isNullOrEmpty()) return emptySet()
        val type = object : TypeToken<Set<Long>>() {}.type
        return gson.fromJson(value, type) ?: emptySet()
    }
}