package com.perevodchik.controllers.http

import com.perevodchik.domain.*
import com.perevodchik.enums.NotificationType
import com.perevodchik.repository.*
import com.perevodchik.utils.DateTimeUtil
import com.perevodchik.utils.FileUtils
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.rules.SecurityRule
import java.lang.Exception
import java.util.*
import javax.inject.Inject

@Controller("/orders")
class OrdersController {

    @Inject
    lateinit var ordersService: OrdersService
    @Inject
    lateinit var usersService: UsersService
    @Inject
    lateinit var mastersService: MastersService
    @Inject
    lateinit var sentenceService: SentenceService
    @Inject
    lateinit var notificationsService: NotificationsService
    @Inject
    lateinit var commentsService: CommentsService
    @Inject
    lateinit var conversionsService: ConversionsService

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Get("/all")
    fun availableOrders(@QueryValue(value = "cities") cities: Optional<String>,
                        @QueryValue(value = "services") services: Optional<String>,
                        @QueryValue(value = "price") price: Optional<Boolean>,
                        @QueryValue(value = "city") city: Optional<Boolean>,
                        @QueryValue(value = "page") page: Int,
                        @QueryValue(value = "limit") limit: Int): List<AvailableOrderPreview> {
        return ordersService.getAvailableOrders(page, limit, cities.orElse(""), services.orElse(""), price.orElse(false), city.orElse(false))
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Get("/{id}")
    fun get(@PathVariable id: Int): HttpResponse<*> {
        val order = ordersService.getOrderById(id, sentenceService) ?: return HttpResponse.notFound(id)
        return HttpResponse.ok(order)
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Get("/user")
    fun clientOrders(authentication: Authentication): List<OrderPreview> {
        return ordersService.getUserOrders(authentication.attributes["id"] as Int, authentication.attributes["role"] as Int)
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Post("/create")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun createOrder(@Body order: OrderShort, authentication: Authentication): HttpResponse<Order> {
        if(authentication.attributes["role"] == 1)
            return HttpResponse.badRequest()
        val id = authentication.attributes["id"] as Int
        println("$order")
        val newOrder = ordersService.createOrder(order, id) ?: return HttpResponse.badRequest()
        if(newOrder.isPrivate) {
            notificationsService.createNotification(
                    Notification(
                            id = 0,
                            userId = newOrder.masterId,
                            secondUserId = newOrder.clientId,
                            orderId = newOrder.id,
                            notificationType = NotificationType.ORDER_NEW_REQUEST.value,
                            createdAt = DateTimeUtil.timestamp()
                    )
            )
        }
        return HttpResponse.ok(newOrder)
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Post("/delete")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun deleteOrder(@Body order: Order, authentication: Authentication): HttpResponse<Order> {
        val client = usersService.getCurrent(authentication)
        if(client != null) {
            return if(order.clientId == client.id) {
                ordersService.deleteOrder(order)
                HttpResponse.ok(order)
            } else HttpResponse.notModified()
        }
        return HttpResponse.badRequest(order)
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Post("/update")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun updateOrder(@Body order: Order): HttpResponse<Order> {
//        val oldOrder = ordersService.getOrderById(order.id) ?: return HttpResponse.notModified()
//        if(order.status == 1) {
//            val client = usersService.getCurrent(authentication) ?: return HttpResponse.notModified()
//            return HttpResponse.ok(ordersService.updateOrder(order))
//        } else if(oldOrder.status == 2) {
//            val client = usersService.getCurrent(authentication) ?: return HttpResponse.notModified()
//            val master = usersService.getById(order.id) ?: return HttpResponse.notModified()
//            return HttpResponse.ok(ordersService.updateOrder(order))
//        } else if(oldOrder.status == 3) {
//            val master = usersService.getCurrent(authentication)
//            if(master != null) {
//                return HttpResponse.ok(ordersService.updateOrder(order))
//            }
//        }
        return HttpResponse.ok(order)
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Post("/status/update")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun updateOrderStatus(@Body payload: UpdateOrdersStatusPayload, authentication: Authentication): HttpResponse<Boolean> {
        val currentUser = authentication.attributes["id"] as Int
        val currentRole = authentication.attributes["role"] as Int
        println("payload is $payload")
        val order = ordersService.getShortOrderById(payload.orderId) ?: return HttpResponse.badRequest()
        println("$order")
        when(payload.status) {
            0 -> {
            }
            // finish the order by client
            1 -> {
                if(order.clientId == currentUser) {
                    val isUpdate = ordersService.updateOrderStatus(payload.orderId, payload.status, payload.masterId)
                    if(isUpdate)
                        notificationsService.createNotification(
                                Notification(
                                        id = 0,
                                        userId = payload.masterId,
                                        secondUserId = currentUser,
                                        orderId = payload.orderId,
                                        notificationType = NotificationType.ORDER_FINISHED_BY_CLIENT.value,
                                        createdAt = DateTimeUtil.timestamp()
                                )
                        )
                    return HttpResponse.ok(isUpdate)
                }
                return HttpResponse.badRequest()
            }
            // select master to order by client
            // or select private order by master
            2 -> {
//                if(currentUser == order.clientId || currentUser == order.masterId) {
                when {
                    order.clientId == currentUser -> {
                        val isUpdate = ordersService.updateOrderStatus(payload.orderId, payload.status, payload.masterId)
                        if (isUpdate) {
                            notificationsService.createNotification(
                                    Notification(
                                            id = 0,
                                            userId = currentUser,
                                            secondUserId = payload.masterId,
                                            orderId = payload.orderId,
                                            notificationType = NotificationType.ORDER_SELECT_BY_MASTER.value,
                                            createdAt = DateTimeUtil.timestamp()
                                    )
                            )
                           conversionsService.createConversion(payload.clientId!!, payload.masterId!!)
                        }
                        println("client ${order.clientId}")
                        return HttpResponse.ok(isUpdate)
                    }
                    order.masterId == currentUser -> {
                        val isUpdate = ordersService.updateOrderStatus(payload.orderId, payload.status, payload.masterId)
                        if(isUpdate) {
                            notificationsService.createNotification(
                                    Notification(
                                            id = 0,
                                            userId = payload.clientId,
                                            secondUserId = currentUser,
                                            orderId = payload.orderId,
                                            notificationType = NotificationType.ORDER_SELECT_BY_MASTER.value,
                                            createdAt = DateTimeUtil.timestamp()
                                    )
                            )
                            conversionsService.createConversion(payload.clientId!!, payload.masterId!!)
                        }
                        println("master ${order.masterId}")
                        return HttpResponse.ok(isUpdate)
                    }
                    else -> {
                        return HttpResponse.badRequest()
                    }
                }
            }
            // send private order to master
//            3 -> {
//                if(currentUser == order.clientId || currentUser == order.masterId) {
//                    if(order.clientId == currentUser)
//                        return HttpResponse.ok(ordersService.updateOrderStatus(payload.orderId, payload.status, payload.masterId))
//                } else {
//                    return HttpResponse.badRequest()
//                }
//            }
            // cancel order
            4 -> {
                if(order.status != 4) {
                    val isUpdate = ordersService.updateOrderStatus(payload.orderId, payload.status, payload.masterId)
                    if(isUpdate) {
                        if(currentRole == 0) {
                            notificationsService.createNotification(
                                    Notification(
                                            id = 0,
                                            userId = currentUser,
                                            secondUserId = payload.masterId,
                                            orderId = payload.orderId,
                                            notificationType = NotificationType.ORDER_CANCELLED_BY_CLIENT.value,
                                            createdAt = DateTimeUtil.timestamp()
                                    )
                            )
                        } else if(currentRole == 1) {
                            notificationsService.createNotification(
                                    Notification(
                                            id = 0,
                                            userId = currentUser,
                                            secondUserId = payload.clientId,
                                            orderId = payload.orderId,
                                            notificationType = NotificationType.ORDER_CANCELLED_BY_MASTER.value,
                                            createdAt = DateTimeUtil.timestamp()
                                    )
                            )
                        }
                    }
                    return HttpResponse.ok(isUpdate)
                }
                return HttpResponse.badRequest()
            }
        }
        return HttpResponse.badRequest()
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Post("/upload")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun addOrderImage(authentication: Authentication, orderId: Int, upload: StreamingFileUpload): HttpResponse<String> {
        ordersService.getOrderById(orderId, sentenceService) ?: return HttpResponse.badRequest("order $orderId not found")
        val fileName = FileUtils().generateFilePathAndName("images", authentication.attributes["username"] as String, upload.name)
        println("create1 $fileName")
        try {
            val uploadResult = ordersService.upload(orderId, fileName, upload)
            println("uploadResult1 $uploadResult")
            if(uploadResult.isNotBlank() && uploadResult.isNotBlank()) {
                return HttpResponse.ok(uploadResult)
            }
            return HttpResponse.badRequest("$orderId")
        } catch(ex: Exception) {
            ex.printStackTrace()
            return HttpResponse.badRequest(ex.localizedMessage)
        }

    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Post("/uploadExist")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    fun addExistingImage(authentication: Authentication, existingImage: ExistingImage): HttpResponse<String> {
        println("create $existingImage")
        ordersService.getOrderById(existingImage.orderId, sentenceService) ?: return HttpResponse.badRequest("order ${existingImage.orderId} not found")
        return HttpResponse.ok(ordersService.addExistingImage(existingImage))
    }
}

