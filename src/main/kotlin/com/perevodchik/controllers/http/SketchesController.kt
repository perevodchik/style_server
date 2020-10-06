package com.perevodchik.controllers.http

import com.perevodchik.domain.MinMax
import com.perevodchik.domain.Sketch
import com.perevodchik.domain.SketchFull
import com.perevodchik.domain.SketchPreview
import com.perevodchik.repository.MastersService
import com.perevodchik.repository.OrdersService
import com.perevodchik.repository.SketchesService
import com.perevodchik.repository.UsersService
import com.perevodchik.utils.FileUtils
import io.micronaut.http.*
import io.micronaut.http.annotation.*
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.rules.SecurityRule
import java.lang.Exception
import java.util.*
import javax.inject.Inject

@Controller("/sketches")
class SketchesController {

    @Inject
    lateinit var sketchesService: SketchesService
    @Inject
    lateinit var mastersService: MastersService
    @Inject
    lateinit var usersService: UsersService
    @Inject
    lateinit var ordersService: OrdersService

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Get("/all")
    fun allSketches(): List<SketchPreview> {
        return sketchesService.getAllSketches()
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Get("/list")
    fun list(@QueryValue(value = "tags") tags: Optional<String>,
             @QueryValue(value = "favorites") isFavorites: Optional<Boolean>,
             @QueryValue(value = "min") minPrice: Optional<Int>,
             @QueryValue(value = "max") maxPrice: Optional<Int>,
             @QueryValue(value = "page") page: Optional<Int>,
             @QueryValue(value = "limit") limit: Optional<Int>): List<SketchPreview> {
        val varPage = page.orElse(0)
        val varLimit = limit.orElse(50)

        val tagsString: String
        val tgs = mutableListOf<String>()
        for(t in tags.orElse("").split(",")) {
            if(t.isNotEmpty() && t.isNotBlank())
                tgs.add("\'${if(t.startsWith("#")) t else "#$t"}\'")
        }
        tagsString = tgs.joinToString(",")
        return sketchesService.getList(varPage * varLimit, varLimit, minPrice.orElse(-1), maxPrice.orElse(-1), tagsString, isFavorites.orElse(false))
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
        println("$sketch")
        return HttpResponse.ok<SketchFull>(sketch)
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Post("/create")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun createSketch(@Body sketch: Sketch, authentication: Authentication): HttpResponse<Sketch> {
        sketch.ownerId = authentication.attributes["id"] as Int
        val newTags = mutableListOf<String>()
        val tags = sketch.tags.split(" ,", ", ", ",", " ")
        for(t in tags) {
            val newTag = if(t.startsWith("#")) t else "#$t"
            newTags.add(newTag)
        }
        sketch.tags = newTags.joinToString(",")
        return HttpResponse.ok(sketchesService.createSketch(sketch))
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
    @Post("/upload")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun addSketchImage(authentication: Authentication, sketchId: Int, upload: StreamingFileUpload): HttpResponse<String> {
        sketchesService.getSketchById(sketchId) ?: return HttpResponse.badRequest("$sketchId")
        val fileName = FileUtils().generateFilePathAndName("sketches", authentication.attributes["username"] as String, upload.name)
        try {
            val uploadResult = sketchesService.upload(sketchId, fileName, upload)
            if(uploadResult.isNotBlank() && !uploadResult.isNotBlank()) {
                println("result not blank")
                return HttpResponse.ok(uploadResult)
            }
            println("result blank")
            return HttpResponse.badRequest("$sketchId")
        } catch(ex: Exception) {
            ex.printStackTrace()
            return HttpResponse.badRequest(ex.localizedMessage)
        }

    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Post("/like")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun like(authentication: Authentication, sketchId: Int): HttpResponse<Boolean> {
        return HttpResponse.ok(sketchesService.likeSketch(sketchId, authentication.attributes["id"] as Int))
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Post("/unlike")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun unlike(authentication: Authentication, sketchId: Int): HttpResponse<Boolean> {
        return HttpResponse.ok(sketchesService.unlikeSketch(sketchId, authentication.attributes["id"] as Int))
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Post("/delete")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun deleteSketch(sketchId: Int, authentication: Authentication): HttpResponse<String>{
        val sketch = sketchesService.getSketchById(sketchId) ?: return HttpResponse.badRequest()
        if(sketch.ownerId != authentication.attributes["id"] as Int)
            return HttpResponse.badRequest()
        val r = sketchesService.deleteSketch(sketchId)
        return if(r) {
            FileUtils().deleteFile("static/${sketch.photos}")
            HttpResponse.ok()
        } else
            HttpResponse.badRequest()
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Post("/prices")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun getMinMaxPrices(): HttpResponse<MinMax> {
        return HttpResponse.ok(sketchesService.getPrices())
    }
}