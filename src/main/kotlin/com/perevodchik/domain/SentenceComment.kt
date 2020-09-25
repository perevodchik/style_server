package com.perevodchik.domain

data class SentenceComment (
        var id: Int,
        var sentenceId: Int,
        var userId: Int,
        var userName: String,
        var userSurname: String,
        var userAvatar: String,
        var message: String,
        var createAt: String
)

data class SentenceCommentShort(
        var sentenceId: Int,
        var message: String
)