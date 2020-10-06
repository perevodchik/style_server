package com.perevodchik.repository.impl

import com.perevodchik.domain.Comment
import com.perevodchik.domain.CommentFull
import com.perevodchik.repository.CommentsService
import com.perevodchik.utils.DateTimeUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommentsServiceImpl: CommentsService {
    @Inject
    lateinit var pool: io.reactiverse.reactivex.pgclient.PgPool
    override fun commentsCount(id: Int): Int {
        val r = pool.rxQuery("SELECT COUNT(id) AS count FROM comments WHERE target_id = $id;").blockingGet()
        if(r.iterator().hasNext())
            return r.iterator().next().getInteger("count")
        return 0
    }

    override fun createComment(comment: Comment, role: Int): Comment? {
        print(comment.toString())
        val createdAt = comment.createdAt ?: DateTimeUtil.timestamp()
        val r = pool.rxQuery("INSERT INTO comments (commentator_id, target_id, message, rate, created_at) VALUES (${comment.commentatorId}, ${comment.targetId}, '${comment.message}', ${comment.rate}, '$createdAt') RETURNING comments.id;").blockingGet()
        val i = r.iterator()
        if(i.hasNext()) {
            val commentId = i.next().getInteger("id")
            comment.id = commentId
            comment.createdAt = createdAt
            pool.rxQuery("UPDATE orders SET ${if(role == 0) "client_comment_id" else "master_comment_id"} = $commentId WHERE id = ${comment.orderId};").blockingGet()
            return comment
        }
        return null
    }

    override fun getCommentById(id: Int): CommentFull? {
        val r = pool
                .rxQuery("SELECT comments.id as comment_id, comments.target_id, comments.commentator_id, " +
                        "users.name, users.surname, users.avatar, " +
                        "comments.rate, comments.message, comments.created_at " +
                        "FROM comments " +
                        "INNER JOIN users ON users.id = comments.commentator_id " +
                        "WHERE target_id = $id;")
                .blockingGet()
        val i = r.iterator()
        if(i.hasNext()) {
            val row = i.next()
            val comment = CommentFull(
                    id = row.getInteger("id"),
                    targetId = row.getInteger("target_id"),
                    commentatorId = row.getInteger("commentator_id"),
                    commentatorName = row.getString("name"),
                    commentatorSurname = row.getString("surname"),
                    commentatorAvatar = row.getString("avatar"),
                    message = row.getString("id"),
                    rate = row.getDouble("rate"),
                    createdAt = row.getString("createdAt") ?: DateTimeUtil.timestamp()
            )
            return comment
        }
        return null
    }

    override fun getCommentsByUserId(id: Int): List<CommentFull> {
        val comments = mutableListOf<CommentFull>()
        val r = pool.rxQuery("SELECT comments.id as comment_id, comments.target_id, comments.commentator_id, users.name, users.surname, users.avatar, comments.rate, comments.message, comments.created_at FROM comments INNER JOIN users ON users.id = comments.commentator_id WHERE target_id = $id;").blockingGet()
        val i = r.iterator()
        while(i.hasNext()) {
            val row = i.next()
            val comment = CommentFull(
                    id = row.getInteger("comment_id"),
                    targetId = row.getInteger("target_id"),
                    commentatorId = row.getInteger("commentator_id"),
                    commentatorName = row.getString("name"),
                    commentatorSurname = row.getString("surname"),
                    commentatorAvatar = row.getString("avatar"),
                    message = row.getString("message"),
                    rate = row.getDouble("rate"),
                    createdAt = row.getString("createdAt") ?: DateTimeUtil.timestamp()
            )
            comments.add(comment)
        }

        return comments
    }

    override fun getCommentsByUserIdLimited(id: Int, limit: Int): List<CommentFull> {
        val comments = mutableListOf<CommentFull>()
        val r = pool.rxQuery("SELECT comments.id as id, comments.target_id, comments.commentator_id, users.name, users.surname, users.avatar, comments.rate, comments.message, comments.created_at FROM comments INNER JOIN users ON users.id = comments.commentator_id WHERE target_id = $id ORDER BY comments.id DESC LIMIT $limit;").blockingGet()
        val i = r.iterator()
        while(i.hasNext()) {
            val row = i.next()
            val comment = CommentFull(
                    id = row.getInteger("id"),
                    targetId = row.getInteger("target_id"),
                    commentatorId = row.getInteger("commentator_id"),
                    commentatorName = row.getString("name"),
                    commentatorSurname = row.getString("surname"),
                    commentatorAvatar = row.getString("avatar"),
                    message = row.getString("message"),
                    rate = row.getDouble("rate"),
                    createdAt = row.getString("createdAt") ?: DateTimeUtil.timestamp()
            )
            comments.add(comment)
        }

        return comments
    }
}