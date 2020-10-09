package com.perevodchik.controllers.http

import com.perevodchik.domain.Phone
import com.perevodchik.domain.User
import com.perevodchik.domain.UserRegistered
import com.perevodchik.domain.UserUpdatePayload
import com.perevodchik.repository.*
import com.perevodchik.security.AuthStorage
import com.perevodchik.utils.FileUtils
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.rules.SecurityRule
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
        val user = usersService.getByPhone(authentication.attributes["username"] as String) ?: return HttpResponse.badRequest()
        print("current is $user")
        return HttpResponse.ok(user)
    }

    @Secured(SecurityRule.IS_ANONYMOUS)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Post("/create")
    fun create(@Body user: UserRegistered): HttpResponse<UserRegistered> {
        val user0 = usersService.create(user) ?: return HttpResponse.badRequest()
        return HttpResponse.ok(user0)
    }

    @Secured(SecurityRule.IS_ANONYMOUS)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Post("/exist")
    fun isUserExist(phone: String, role: Int): HttpResponse<User> {
        val user = usersService.getByPhone(phone) ?: return HttpResponse.badRequest()
        if(user.role != role) return HttpResponse.badRequest()
        AuthStorage.createCode(Phone(phone))
        return HttpResponse.ok()
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Get("/full/{id}")
    fun getFullById(authentication: Authentication, @PathVariable id: Int): HttpResponse<User> {
        val currentUserId = authentication.attributes["id"] as Int
        val user = usersService.getById(id) ?: return HttpResponse.badRequest()
        println("$user")
        val categories = categoriesService.getAll()
        val services = mastersService.getServiceByMaster(id)

        user.services = mastersService.getFullServiceByMaster(categories, services)
        user.photos = mastersService.getMasterPhotos(user.id)
        user.commentsCount = commentsService.commentsCount(id)
        user.comments = commentsService.getCommentsByUserIdLimited(id, 2).toMutableList()
        if(currentUserId != id) {
            user.isRecorded = ordersService.isClientRecordedToMaster(currentUserId, id)
            if(!user.isShowAddress || !user.isRecorded)
                user.address = ""
            if(!user.isShowPhone || !user.isRecorded)
                user.phone = ""
            if(!user.isShowEmail || !user.isRecorded)
                user.email = ""
        }
        return HttpResponse.ok(user)
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Get("/{id}")
    fun getById(authentication: Authentication, @PathVariable id: Int): HttpResponse<User> {
        val currentUserId = authentication.attributes["id"] as Int
        val user = usersService.getById(id) ?: return HttpResponse.badRequest()
        if(currentUserId != id) {
            user.isRecorded = ordersService.isClientRecordedToMaster(currentUserId, id)
            if(!user.isShowAddress || !user.isRecorded)
                user.address = ""
            if(!user.isShowPhone || !user.isRecorded)
                user.phone = ""
            if(!user.isShowEmail || !user.isRecorded)
                user.email = ""
        }
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
    fun update(user: UserUpdatePayload, authentication: Authentication): HttpResponse<UserUpdatePayload> {
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
    fun uploadAvatar(upload: StreamingFileUpload, authentication: Authentication): HttpResponse<String> {
        val fileName = FileUtils().generateFilePathAndName("avatars", authentication.attributes["username"] as String, upload.name)
        println("fileName $fileName")
        val result = usersService.upload(authentication.attributes["username"] as String, fileName, upload)
        return if(result.isNotBlank() && result.isNotEmpty())
            HttpResponse.ok(result)
        else HttpResponse.badRequest()
    }
}