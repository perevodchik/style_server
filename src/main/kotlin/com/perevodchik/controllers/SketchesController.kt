package com.perevodchik.controllers

import com.perevodchik.domain.Sketch
import com.perevodchik.domain.SketchFull
import com.perevodchik.domain.SketchPreview
import com.perevodchik.repository.MastersService
import com.perevodchik.repository.SketchesService
import com.perevodchik.repository.UsersService
import com.perevodchik.utils.DateTimeUtil
import io.micronaut.http.*
import io.micronaut.http.annotation.*
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.rules.SecurityRule
import java.lang.Exception
import javax.inject.Inject

@Controller("/sketches")
class SketchesController {

    @Inject
    lateinit var sketchesService: SketchesService
    @Inject
    lateinit var mastersService: MastersService
    @Inject
    lateinit var usersService: UsersService

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Get("/all")
    fun allSketches(): List<SketchPreview> {
        return sketchesService.getAllSketches()
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Get("/list")
    fun list(authentication: Authentication, @QueryValue(value = "page") page: Int, @QueryValue(value = "limit") limit: Int): List<SketchPreview> {
        return sketchesService.getList(page * limit, limit)
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Get("/master/{id}")
    fun masterSketches(@PathVariable id: Int): List<SketchPreview> {
        return sketchesService.getSketchesByMaster(id)
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Get("/{id}")
    fun sketchyId(@PathVariable id: Int, authentication: Authentication): HttpResponse<SketchFull> {
        val sketch = sketchesService.getSketchById(id) ?: return HttpResponse.notFound<SketchFull>()
        sketch.isFavorite = sketchesService.isSketchLiked(id, authentication.attributes["id"] as Int)
        return HttpResponse.ok<SketchFull>(sketch)
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Post("/create")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun createSketch(@Body sketch: Sketch, authentication: Authentication): HttpResponse<Sketch> {
        println("create $sketch")
        val master = usersService.getByPhone(authentication.attributes["username"] as String) ?: return HttpResponse.badRequest(sketch)
        return if(sketch.ownerId == master.id) {
            HttpResponse.ok(sketchesService.createSketch(sketch))
        }
        else HttpResponse.badRequest(sketchesService.createSketch(sketch))
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Post("/update")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun updateOrder(@Body sketch: Sketch, authentication: Authentication): HttpResponse<Sketch> {
        val master = usersService.getCurrent(authentication) ?: return HttpResponse.badRequest(sketch)
        if(master.id != sketch.ownerId)
            return HttpResponse.notModified()
        return HttpResponse.ok(sketchesService.updateSketch(sketch))
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Post("/delete")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun removeOrder(@Body sketch: Sketch, authentication: Authentication): HttpResponse<Sketch> {
        val master = usersService.getCurrent(authentication) ?: return HttpResponse.badRequest(sketch)
        if(master.id != sketch.ownerId)
            return HttpResponse.notModified()
        return if(sketchesService.removeSketch(sketch))
            HttpResponse.ok()
        else HttpResponse.notAllowed()
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Post("/upload")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun addSketchImage(authentication: Authentication, sketchId: Int, upload: StreamingFileUpload): HttpResponse<*> {
        val master = usersService.getByPhone(authentication.attributes["username"] as String) ?: return HttpResponse.badRequest(sketchId)
        val sketch = sketchesService.getSketchById(sketchId) ?: return HttpResponse.badRequest(sketchId)
        if(master.id != sketch.ownerId) return HttpResponse.badRequest(sketchId)
        val fileName = "${master.phone.hashCode()}_${DateTimeUtil.timestamp().hashCode()}"
                .replace("-", "")
                .replace(" ", "")
                .replace(".", "")
                .replace(":", "")
                .plus(".jpeg")
        try {
            val isUpload = sketchesService.upload(sketchId, fileName, upload)
            if(isUpload) {
                val r = sketchesService.createSketchImage(fileName, sketchId)
                return if(!r)
                    HttpResponse.badRequest(sketchId)
                else HttpResponse.ok(fileName)
            }
            return HttpResponse.badRequest(sketchId)
        } catch(ex: Exception) {
            return HttpResponse.badRequest(ex.localizedMessage)
        }
    }
}