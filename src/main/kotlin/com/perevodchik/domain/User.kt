package com.perevodchik.domain

data class User(
        var id: Int,
        var cityId: Int,
        var role: Int,
        var commentsCount: Int = 0,
        var phone: String,
        var name: String,
        var surname: String,
        var email: String,
        var address: String,
        var avatar: String = "",
        var photos: String = "",
        var isShowAddress: Boolean = true,
        var isShowPhone: Boolean = true,
        var isShowEmail: Boolean = true,
        var isRecorded: Boolean = false,
        var services: List<Category> = mutableListOf(),
        var comments: List<CommentFull> = mutableListOf()
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

data class UserShort(
        var id: Int,
        var name: String,
        var surname: String,
        var avatar: String
)