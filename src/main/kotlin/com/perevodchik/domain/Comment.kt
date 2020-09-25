package com.perevodchik.domain

data class Comment (
        var id: Int,
        var commentatorId: Int,
        var targetId: Int,
        var orderId: Int,
        var message: String,
        var rate: Double,
        var createdAt: String?
)

data class CommentFull (
        var id: Int,
        var commentatorId: Int,
        var targetId: Int,
        var commentatorName: String,
        var commentatorSurname: String,
        var commentatorAvatar: String,
        var message: String,
        var rate: Double,
        var createdAt: String
)