package com.louislu.pennbioinformatics.data.session.remote

import com.google.gson.annotations.SerializedName

data class CreateSessionRequest(
    @SerializedName("user_id") val userId: String,
    @SerializedName("group_name") val groupName: String?,
    @SerializedName("class_name") val className: String,
    @SerializedName("school_name") val schoolName: String,
    @SerializedName("device_name") val deviceName: String?,
    @SerializedName("start_timestamp") val startTimestamp: Long,
    val title: String,
    val description: String?
)