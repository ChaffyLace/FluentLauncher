package com.example.fluentui.ui.database

import androidx.room.TypeConverter
import com.example.fluentui.ui.components.AppModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromAppModelList(value: List<AppModel>?): String {
        return gson.toJson(value ?: emptyList<AppModel>())
    }

    @TypeConverter
    fun toAppModelList(value: String?): List<AppModel> {
        if (value == null) return emptyList()
        val type = object : TypeToken<List<AppModel>>() {}.type
        return gson.fromJson(value, type)
    }
}