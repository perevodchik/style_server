package com.perevodchik.controllers.http

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import java.io.File

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/images")
class FilesController {
    @Secured(SecurityRule.IS_ANONYMOUS)
    @Get("/avatar/{path}")
    fun getAvatar(@PathVariable path: String): File {
        return File("static/avatars/$path")
    }

    @Secured(SecurityRule.IS_ANONYMOUS)
    @Get("/{folder}/{level0}/{level1}/{file}")
    fun getMediaImage(@PathVariable folder: String,
                      @PathVariable level0: String,
                      @PathVariable level1: String,
                      @PathVariable file: String): File {
        return File("static/$folder/$level0/$level1/$file")
    }
}