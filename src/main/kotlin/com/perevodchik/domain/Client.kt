package com.perevodchik.domain

data class Client(
        var id: Int,
        var cityId: Int,
        var phone: String,
        var name: String,
        var surname: String,
        var email: String,
        var address: String,
        var avatar: String,
        var isShowAddress: Boolean,
        var isShowPhone: Boolean,
        var isShowEmail: Boolean,
        var comments: MutableList<Comment> = mutableListOf()
)