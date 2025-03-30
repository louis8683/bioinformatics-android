package com.louislu.pennbioinformatics.auth

data class UpdateUserInfoRequest(
    val access_token: String,
    val group_name: String,
    val class_name: String,
    val school_name: String
)