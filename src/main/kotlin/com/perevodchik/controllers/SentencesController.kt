package com.perevodchik.controllers

import com.perevodchik.domain.*
import com.perevodchik.enums.NotificationType
import com.perevodchik.repository.NotificationsService
import com.perevodchik.repository.OrdersService
import com.perevodchik.repository.SentenceService
import com.perevodchik.utils.DateTimeUtil
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.rules.SecurityRule
import javax.inject.Inject

@Controller("/sentences")
class SentencesController {
    @Inject
    lateinit var ordersService: OrdersService
    @Inject
    lateinit var sentenceService: SentenceService
    @Inject
    lateinit var notificationsService: NotificationsService

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Post("/test/{orderId}")
    fun test(@PathVariable orderId: Int,  authentication: Authentication): HttpResponse<*> {
        println("test")
        return HttpResponse.ok(sentenceService.test(orderId))
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Post("/create")
    fun createSentence(sentence: Sentence, authentication: Authentication): HttpResponse<*> {
        val createdSentence = sentenceService.create(sentence)
        notificationsService.createNotification(
                Notification(
                        id = 0,
                        userId = ordersService.getShortOrderById(createdSentence.orderId)?.clientId,
                        secondUserId = authentication.attributes["id"] as Int,
                        orderId = createdSentence.orderId,
                        notificationType = NotificationType.ORDER_NEW_SENTENCE.value,
                        createdAt = DateTimeUtil.timestamp()
                )
        )
        return HttpResponse.ok(createdSentence)
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Get("/{orderId}")
    fun getByOrder(@PathVariable orderId: Int, authentication: Authentication): HttpResponse<List<SentenceFull>> {
        return HttpResponse.ok(sentenceService.getSentencesByOrder(orderId))
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Post("/comments")
    fun createSentenceComment(@Body sentenceComment: SentenceCommentShort, authentication: Authentication): HttpResponse<SentenceComment> {
        println("create comment $sentenceComment")
        val sentence = sentenceService.createSentenceComment(authentication.attributes["id"] as Int, sentenceComment) ?: return HttpResponse.badRequest()
        println("sentence $sentence")
        return HttpResponse.ok(sentence)
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Get("/comments/{sentenceId}")
    fun getComments(@PathVariable sentenceId: Int, authentication: Authentication): HttpResponse<List<SentenceComment>> {
        return HttpResponse.ok(sentenceService.getComments(sentenceId))
    }

}