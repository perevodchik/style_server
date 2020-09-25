package com.perevodchik.controllers

import com.perevodchik.domain.Style
import com.perevodchik.repository.StylesService
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import javax.inject.Inject

@Secured(SecurityRule.IS_AUTHENTICATED)
@Controller("styles")
class StyleController {

    @Inject
    lateinit var stylesService: StylesService

    @Get("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun positions(request: HttpRequest<*>): HttpResponse<List<Style>> {
        return HttpResponse.ok(stylesService.getAllStyles())
    }

    @Post("/create")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun createPosition(@Body style: Style): HttpResponse<Style> {
        return HttpResponse.ok(stylesService.createStyle(style))
    }

}