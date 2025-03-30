package com.louislu.pennbioinformatics.auth.appauthhelper

import androidx.datastore.core.Serializer
import com.louislu.pennbioinformatics.domain.model.UserInfo
import org.json.JSONObject
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream

object UserInfoSerializer : androidx.datastore.core.Serializer<UserInfo?> {
    override val defaultValue: UserInfo? = null

    override suspend fun readFrom(input: java.io.InputStream): UserInfo? {
        return try {
            val jsonString = input.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            UserInfo.fromJson(jsonObject)
        } catch (e: Exception) {
            Timber.e("Error reading from DataStore: $e")
            null
        }
    }

    override suspend fun writeTo(t: UserInfo?, output: java.io.OutputStream) {
        output.bufferedWriter().use { it.write(t?.toJson()?.toString() ?: "") }
    }
}
