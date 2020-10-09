package com.perevodchik.controllers.http

import com.perevodchik.utils.FileUtils
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
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

    @Secured(SecurityRule.IS_ANONYMOUS)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post("/upload")
    fun uploadAvatar(upload: StreamingFileUpload, quality: Float, username: String): HttpResponse<String> {
        val fileName = FileUtils().generateFilePathAndName("test", username, upload.name)
        FileUtils().uploadFileTest(upload, fileName, quality)
        return HttpResponse.ok(fileName)
    }
}