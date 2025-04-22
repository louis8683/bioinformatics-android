package com.louislu.pennbioinformatics.data.session.remote

data class UpdateSessionRequest(
    val description: String? = null,
    val device_name: String? = null,
    val group_name: String? = null,
    val class_name: String? = null,
    val school_name: String? = null,
    val title: String? = null,
    val end_timestamp: Long? = null
)