package com.louislu.pennbioinformatics.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStore
import com.louislu.pennbioinformatics.auth.appauthhelper.UserInfoSerializer
import com.louislu.pennbioinformatics.domain.model.UserInfo

val Context.userInfoDataStore: DataStore<UserInfo?> by dataStore(
    fileName = "user_info.json",
    serializer = UserInfoSerializer,
    corruptionHandler = ReplaceFileCorruptionHandler(
        produceNewData = { null }
    )
)
