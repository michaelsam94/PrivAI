package com.michael.privai.data.database

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        if (value == null) return null
        return value.joinToString(separator = "|||")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value == null) return null
        if (value.isEmpty()) return emptyList()
        return value.split("|||")
    }
}
