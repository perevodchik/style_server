package com.perevodchik.controllers.http

import com.perevodchik.repository.PositionService
import com.perevodchik.domain.Position
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import javax.inject.Inject

@Secured(SecurityRule.IS_AUTHENTICATED)
@Controller("/positions")
class PositionController() {

    init {
        println("PositionController")
    }

    @Inject
    lateinit var positionService: PositionService

    @Get("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun positions(): List<Position> {
        return positionService.getAllPositions()
    }

    @Post("/create")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun createPosition(@Body position: Position): HttpResponse<Position> {
        return HttpResponse.ok(positionService.createPosition(position))
                .contentType(MediaType.APPLICATION_JSON)

    }

}