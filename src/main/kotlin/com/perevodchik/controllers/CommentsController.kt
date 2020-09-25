package com.perevodchik.controllers

import com.perevodchik.domain.Comment
import com.perevodchik.domain.Notification
import com.perevodchik.enums.NotificationType
import com.perevodchik.repository.CommentsService
import com.perevodchik.repository.NotificationsService
import com.perevodchik.utils.DateTimeUtil
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.rules.SecurityRule
import javax.inject.Inject

@Controller("/comments")
class CommentsController {

    @Inject
    lateinit var commentsService: CommentsService
    @Inject
    lateinit var notificationsService: NotificationsService

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Post("/create")
    fun createComment(@Body comment: Comment, authentication: Authentication): HttpResponse<Comment> {
        val userId = authentication.attributes["id"] as Int
        val createdComment = commentsService.createComment(comment) ?: return HttpResponse.badRequest()
        println("$createdComment")
        notificationsService.createNotification(
                Notification(
                        id = 0,
                        userId = comment.targetId,
                        secondUserId = userId,
                        orderId = null,
                        notificationType = NotificationType.PROFILE_NEW_COMMENT.value,
                        createdAt = DateTimeUtil.timestamp()
                )
        )
        return HttpResponse.ok(createdComment)
    }
}