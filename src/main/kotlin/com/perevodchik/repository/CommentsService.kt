package com.perevodchik.repository

import com.perevodchik.domain.Comment
import com.perevodchik.domain.CommentFull

interface CommentsService {
    fun commentsCount(id: Int): Int
    fun createComment(comment: Comment): Comment?
    fun getCommentsByUserId(id: Int): List<CommentFull>
    fun getCommentsByUserIdLimited(id: Int, limit: Int): List<CommentFull>
}