package com.perevodchik.controllers.http

import com.perevodchik.domain.Category
import com.perevodchik.domain.Service
import com.perevodchik.repository.CategoriesService
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import javax.inject.Inject

@Controller("/categories")
class CategoriesController {

    @Inject
    lateinit var categoriesService: CategoriesService

    @Secured(SecurityRule.IS_ANONYMOUS)
    @Produces(MediaType.APPLICATION_JSON)
    @Get("/all")
    fun getAll(): MutableList<Category> {
        return categoriesService.getAll()
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Produces(MediaType.APPLICATION_JSON)
    @Post("/category")
    fun createCategory(@Body category: Category): Category {
        return categoriesService.createCategory(category)
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Produces(MediaType.APPLICATION_JSON)
    @Post("/service")
    fun createService(@Body service: Service): Service {
        return categoriesService.createService(service)
    }

}