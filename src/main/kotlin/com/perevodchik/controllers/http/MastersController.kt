package com.perevodchik.controllers.http

import com.perevodchik.domain.*
import com.perevodchik.repository.CategoriesService
import com.perevodchik.repository.UsersService
import com.perevodchik.repository.MastersService
import com.perevodchik.repository.OrdersService
import com.perevodchik.utils.DateTimeUtil
import com.perevodchik.utils.FileUtils
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.rules.SecurityRule
import java.security.Principal
import java.util.*
import javax.inject.Inject

@Secured(SecurityRule.IS_ANONYMOUS)
@Controller("/masters")
class MastersController {

    @Inject
    lateinit var mastersService: MastersService
    @Inject
    lateinit var usersService: UsersService
    @Inject
    lateinit var categoriesService: CategoriesService

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Get("/list")
    fun get(@QueryValue(value = "page") page: Int,
            @QueryValue(value = "limit") limit: Int,
            @QueryValue(value = "cities") cities: Optional<String>,
            @QueryValue(value = "services") services: Optional<String>,
            @QueryValue(value = "rate") rate: Optional<Boolean>
            ): HttpResponse<List<UserShortData>> {
        println("[${cities.orElse("")}] [${cities.isPresent}] [${cities.isEmpty}]")
        println("[${services.orElse("")}] [${services.isPresent}] [${services.isEmpty}]")
        println("[${rate.orElse(false)}] [${rate.isPresent}] [${rate.isEmpty}]")
        return HttpResponse.ok(usersService.getMasters(page * limit, limit, cities.orElse(""), services.orElse(""), rate.orElse(false)))
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Get("/services/{id}")
    fun getMasterServices(@PathVariable id: Int): List<Category> {
        val categories = categoriesService.getAll()
        val services = mastersService.getServiceByMaster(id)
        return mastersService.getFullServiceByMaster(categories, services)
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Post("/services/create")
    fun addMasterService(@Body service: ServiceWrapper, authentication: Authentication): HttpResponse<ServiceWrapper> {
        usersService.getByPhone(authentication.attributes["username"] as String) ?: return HttpResponse.badRequest()
        service.userId = authentication.attributes["id"] as Int
        return HttpResponse.ok(mastersService.addService(service))
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Post("/services/update")
    fun updateMasterService(@Body service: ServiceWrapper, authentication: Authentication): HttpResponse<ServiceWrapper> {
        println("update $service")
        return if(mastersService.checkService(authentication.attributes["id"] as Int, service.id))
            HttpResponse.ok(mastersService.updateService(service))
        else HttpResponse.badRequest(service)
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Post("/services/delete")
    fun deleteMasterService(@Body service: ServiceWrapper, authentication: Authentication): HttpResponse<Int> {
        usersService.getByPhone(authentication.attributes["username"] as String) ?: return  HttpResponse.badRequest()
        return if(mastersService.checkService(authentication.attributes["id"] as Int, service.id))
            HttpResponse.ok(mastersService.deleteService(service))
        else HttpResponse.badRequest(service.id)
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Post("/portfolio/create")
    fun createPortfolioItem(upload: StreamingFileUpload, authentication: Authentication): HttpResponse<PortfolioItem> {
        val master = usersService.getByPhone(authentication.attributes["username"] as String) ?: return HttpResponse.badRequest()
        val fileName = FileUtils().generateFilePathAndName("portfolios", authentication.attributes["username"] as String, upload.name)
        val item = mastersService.addMasterPortfolio(master.id, fileName, upload)
        return if(item != null)
            HttpResponse.ok(item)
        else
            HttpResponse.badRequest()
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Get("/portfolio/{id}")
    fun getMasterPortfolio(@PathVariable id: Int): HttpResponse<*> {
        return HttpResponse.ok(mastersService.getMasterPortfolio(id))
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Post("/portfolio/delete")
    fun deletePortfolioItem(portfolioId: Int, authentication: Authentication): HttpResponse<*> {
        val master = usersService.getByPhone(authentication.attributes["username"] as String) ?: return HttpResponse.badRequest(portfolioId)
        return if(mastersService.deleteMasterPortfolio(master.id, portfolioId))
            HttpResponse.ok(portfolioId)
        else
            HttpResponse.badRequest(portfolioId)
    }

}