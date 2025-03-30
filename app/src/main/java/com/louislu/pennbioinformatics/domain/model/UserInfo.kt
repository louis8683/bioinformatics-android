package com.louislu.pennbioinformatics.domain.model

import org.json.JSONObject

data class UserInfo(
    val userId: String,
    val className: String?,
    val groupName: String?,
    val schoolName: String?,
    val email: String?,
    val familyName: String,
    val givenName: String,
    val nickname: String
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("userId", userId)
            put("className", className)
            put("groupName", groupName)
            put("schoolName", schoolName)
            put("email", email)
            put("familyName", familyName)
            put("givenName", givenName)
            put("nickname", nickname)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): UserInfo {
            return UserInfo(
                userId = json.getString("userId"),
                className = if (json.has("className")) json.getString("className") else null,
                groupName = if (json.has("groupName")) json.getString("groupName") else null,
                schoolName = if (json.has("schoolName")) json.getString("schoolName") else null,
                email = if (json.has("email")) json.getString("email") else null,
                familyName = json.getString("familyName"),
                givenName = json.getString("givenName"),
                nickname = json.getString("nickname")
            )
        }
    }
}