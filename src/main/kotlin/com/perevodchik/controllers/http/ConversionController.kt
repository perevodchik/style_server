package com.perevodchik.controllers.http

import com.perevodchik.controllers.socket.SocketManager
import com.perevodchik.domain.ConversionPreview
import com.perevodchik.domain.Message
import com.perevodchik.repository.ConversionsService
import com.perevodchik.repository.OrdersService
import com.perevodchik.utils.FileUtils
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.rules.SecurityRule
import io.vertx.core.json.JsonObject
import java.util.*
import javax.inject.Inject

@Secured(SecurityRule.IS_AUTHENTICATED)
@Controller("/conversions")
class ConversionController {

    @Inject
    lateinit var conversionsService: ConversionsService
    @Inject
    lateinit var ordersService: OrdersService

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Get("/get")
    fun getConversions(authentication: Authentication): HttpResponse<*> {
        val conversions = conversionsService.getConversions(authentication.attributes["id"] as Int, authentication.attributes["role"] == 1)
        return HttpResponse.ok(conversions)
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Post("/conversion")
    fun getConversion(userId: Int, authentication: Authentication): HttpResponse<ConversionPreview> {
        val conversion = if(authentication.attributes["role"] as Int == 0)
            conversionsService.getConversion(authentication.attributes["id"] as Int, userId)
        else
            conversionsService.getConversion(userId, authentication.attributes["id"] as Int)
        return if(conversion.isPresent)
            HttpResponse.ok(conversion.get())
        else
            HttpResponse.badRequest()
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Get("/{conversionId}")
    fun getMessages(@PathVariable conversionId: Int,
                    @QueryValue(value = "page") page: Optional<Int>,
                    @QueryValue(value = "limit") limit: Optional<Int>,
                    authentication: Authentication): HttpResponse<JsonObject> {
        val userId = authentication.attributes["id"] as Int
        val conversionData = conversionsService.getMessagesByConversion(conversionId, userId, page, limit)
        val conversion = conversionsService.getConversion(conversionId, authentication.attributes["role"] as Int == 1)
        println("conversion [${conversion.isPresent}] [${conversion.isEmpty}]")
        if(conversion.isPresent) {
            conversionData.put("canSendMessages", ordersService.isClientRecordedToMaster(userId, conversion.get().user!!.id))
        } else
            conversionData.put("canSendMessages", false)

        println("$conversionData")

        return HttpResponse.ok(conversionData)
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Post("/send")
    fun sendMessage(@Body message: Message): HttpResponse<Message> {
        val sendMessage = conversionsService.sendMessage(message)
        SocketManager.manager().sendMessage(message, message.receiverId)
        if(!sendMessage.isPresent)
            return HttpResponse.badRequest()
        return HttpResponse.ok(sendMessage.get())
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Post("/read")
    fun readMessage(conversionId: Int, messageId: Int, authentication: Authentication): HttpResponse<Any> {
        conversionsService.read(conversionId, authentication.attributes["id"] as Int, messageId)
        return HttpResponse.ok()
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Post("/media")
    fun upload(upload: StreamingFileUpload, authentication: Authentication): HttpResponse<String> {
        val fileName = FileUtils().generateFilePathAndName("messages", authentication.attributes["username"] as String, upload.name)
        return try {
            val uploadResult = conversionsService.upload(fileName, upload)
            if(uploadResult.length > 1) HttpResponse.ok(uploadResult) else HttpResponse.badRequest()
        } catch(e: Exception) {
            e.printStackTrace()
            HttpResponse.badRequest()
        }
    }
}