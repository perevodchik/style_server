package com.perevodchik.domain

import com.perevodchik.utils.DateTimeUtil

data class Sentence(
        var id: Int,
        var orderId: Int,
        var masterId: Int,
        var price: Int,
        var message: String,
        var createdAt: String = DateTimeUtil.timestamp()
)

data class SentenceFull(
        var id: Int,
        var orderId: Int,
        var masterId: Int,
        var price: Int,
        var commentsCount: Int,
        var message: String,
        var masterName: String = "",
        var masterSurname: String = "",
        var masterAvatar: String = "",
        var createdAt: String
)