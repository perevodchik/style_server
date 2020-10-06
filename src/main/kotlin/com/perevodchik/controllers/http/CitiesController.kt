package com.perevodchik.controllers.http

import com.perevodchik.repository.CitiesService
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.Produces
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import javax.inject.Inject

@Controller("/cities")
class CitiesController {

    @Inject
    lateinit var citiesService: CitiesService

    @Secured(SecurityRule.IS_ANONYMOUS)
    @Produces(MediaType.APPLICATION_JSON)
    @Get("/all")
    fun getCities(): HttpResponse<*> {
        return HttpResponse.ok(citiesService.get())
    }

}