package com.louislu.pennbioinformatics.data.entry.remote

import com.google.gson.annotations.SerializedName
import com.louislu.pennbioinformatics.domain.model.DataEntry

data class DataEntryUploadItem(
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("co_level") val coLevel: Float?,
    @SerializedName("pm2_5_level") val pm25Level: Float?,
    val temperature: Float?,
    val humidity: Float?
)

fun DataEntry.toUploadItem(): DataEntryUploadItem {
    return DataEntryUploadItem(
        timestamp = timestamp,
        latitude = latitude ?: 0.0,
        longitude = longitude ?: 0.0,
        coLevel = coLevel,
        pm25Level = pm25level,
        temperature = temperature,
        humidity = humidity
    )
}

fun List<DataEntry>.toUploadItems(): List<DataEntryUploadItem> {
    return this.map { it.toUploadItem() }
}