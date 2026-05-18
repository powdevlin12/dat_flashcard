package com.dttrn.datfs.core.data.local.converter

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * TypeConverter để lưu List<String> (tags) dưới dạng JSON string trong Room.
 */
class StringListConverter {

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromString(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return try {
            json.decodeFromString<List<String>>(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromList(list: List<String>?): String {
        return json.encodeToString(list ?: emptyList())
    }
}
