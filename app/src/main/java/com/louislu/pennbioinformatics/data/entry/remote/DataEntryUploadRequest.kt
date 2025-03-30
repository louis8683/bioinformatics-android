package com.louislu.pennbioinformatics.data.entry.remote

import com.google.gson.annotations.SerializedName

data class DataEntryUploadRequest(
    @SerializedName("data_entries")
    val entries: List<DataEntryUploadItem>
)