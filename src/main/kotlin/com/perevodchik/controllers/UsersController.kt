package com.perevodchik.controllers

import com.perevodchik.domain.User
import com.perevodchik.repository.*
import com.perevodchik.utils.DateTimeUtil
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.rules.SecurityRule
import java.security.Principal
import javax.inject.Inject

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/users")
class UsersController {

    @Inject
    lateinit var mastersService: MastersService
    @Inject
    lateinit var usersService: UsersService
    @Inject
    lateinit var categoriesService: CategoriesService
    @Inject
    lateinit var commentsService: CommentsService
    @Inject
    lateinit var ordersService: OrdersService

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Get("/current")
    fun current(authentication: Authentication): HttpResponse<User> {
        for(s in authentication.attributes)
            println("${s.key} | ${s.value}")
        val user = usersService.getByPhone(authentication.attributes["username"] as String) ?: return HttpResponse.badRequest()
        return HttpResponse.ok(user)
    }

    @Secured(SecurityRule.IS_ANONYMOUS)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Post("/create")
    fun create(@Body user: User): HttpResponse<User> {
        val user0 = usersService.create(user) ?: return HttpResponse.badRequest()
        return HttpResponse.ok(user0)
    }

    @Secured(SecurityRule.IS_ANONYMOUS)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Post("/exist")
    fun isUserExist(phone: String, role: Int): HttpResponse<User> {
        val client = usersService.getByPhone(phone) ?: return HttpResponse.badRequest()
        if(client.role != role) return HttpResponse.badRequest()
        return HttpResponse.ok()
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Get("/full/{id}")
    fun getFullById(authentication: Authentication, @PathVariable id: Int): HttpResponse<User> {
        val user = usersService.getById(id) ?: return HttpResponse.badRequest()
        val categories = categoriesService.getAll()
        val services = mastersService.getServiceByMaster(id)

        user.services = mastersService.getFullServiceByMaster(categories, services)
        user.photos = mastersService.getMasterPhotos(user.id)
        user.commentsCount = commentsService.commentsCount(id)
        user.comments = commentsService.getCommentsByUserIdLimited(id, 3).toMutableList()
        if((authentication.attributes["id"] as Int) != id) {
            val currentRole = (authentication.attributes["role"] as Int)
            if(user.role != currentRole) {
                user.isRecorded = ordersService.isClientRecordedToMaster(user.id, id)
            }
            if(!user.isShowAddress)
                user.address = ""
            if(!user.isShowPhone)
                user.phone = ""
            if(!user.isShowEmail)
                user.email = ""
        }

        println("__ $user __")

        return HttpResponse.ok(user)
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Get("/{id}")
    fun getById(@PathVariable id: Int): HttpResponse<User> {
        val user = usersService.getById(id) ?: return HttpResponse.badRequest()
        return HttpResponse.ok(user)
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Get("/{phone}")
    fun getByPhone(@PathVariable phone: String): HttpResponse<*> {
        return HttpResponse.ok(usersService.getByPhone(phone))
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Post(value = "/update")
    fun update(user: User, authentication: Authentication): HttpResponse<User> {
        return HttpResponse.ok(usersService.update(user, authentication))
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Post(value = "/privacy/update")
    fun updatePrivateSettings(setting: Int, value: Boolean, authentication: Authentication): HttpResponse<*> {
        val isUpdate = usersService.updatePrivateSettings(setting, value, authentication)
        return if(isUpdate)
            HttpResponse.ok(setting)
        else HttpResponse.badRequest("setting $setting not found")
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Post("/avatar")
    fun uploadAvatar(upload: StreamingFileUpload, principal: Principal) {
        println("0")
        val name = "${upload.name.hashCode()}${principal.name.split(":")[1].hashCode()}${DateTimeUtil.timestamp()}"
        println("1")
        usersService.upload(principal.name.split(":")[1], name, upload)
        println("2")
    }
}