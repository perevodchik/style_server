package com.perevodchik.domain

import io.vertx.core.json.JsonObject

data class User(
        var id: Int,
        var cityId: Int,
        var role: Int,
        var commentsCount: Int = 0,
        var rate: Double = 0.0,
        var phone: String,
        var name: String,
        var surname: String,
        var email: String,
        var address: String,
        var about: String = "",
        var avatar: String = "",
        var photos: String = "",
        var isShowAddress: Boolean = true,
        var isShowPhone: Boolean = true,
        var isShowEmail: Boolean = true,
        var isRecorded: Boolean = false,
        var services: List<Category> = mutableListOf(),
        var comments: List<CommentFull> = mutableListOf()
)

data class UserUpdatePayload(
        var cityId: Int,
        var name: String,
        var surname: String,
        var email: String,
        var address: String,
        var about: String
)

data class UserShortData(
        var id: Int,
        var cityId: Int,
        var rate: Double,
        var name: String,
        var surname: String,
        var avatar: String,
        var portfolio: String
)

data class UserRegistered(
        var id: Int,
        var role: Int,
        var name: String,
        var surname: String,
        var phone: String,
        var cityId: Int
)

data class UserShort(
        var id: Int,
        var name: String,
        var surname: String,
        var avatar: String
) {
    fun toJson(): String {
        val json = JsonObject()
        json.put("id", id)
        json.put("name", name)
        json.put("surname", surname)
        json.put("avatar", avatar)
        return json.toString()
    }
}