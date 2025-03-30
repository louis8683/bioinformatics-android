package com.louislu.pennbioinformatics.data.entry.remote

import com.google.gson.annotations.SerializedName

data class DataEntryUploadResponse(
    val message: String,
    @SerializedName("inserted_ids") val insertedIds: List<Long>
)
