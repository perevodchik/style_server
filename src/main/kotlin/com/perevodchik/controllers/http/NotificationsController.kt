package com.perevodchik.controllers.http

import com.perevodchik.domain.NotificationFull
import com.perevodchik.repository.NotificationsService
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.rules.SecurityRule
import javax.inject.Inject


@Controller("/notifications")
class NotificationsController {

    @Inject
    lateinit var notificationsService: NotificationsService


    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Get("/list")
    fun get(authentication: Authentication): HttpResponse<List<NotificationFull>> {
        val list = notificationsService.getNotifications(authentication.attributes["id"] as Int)
        return HttpResponse.ok(list)
    }

}