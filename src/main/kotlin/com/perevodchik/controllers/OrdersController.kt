package com.perevodchik.controllers

import com.perevodchik.domain.*
import com.perevodchik.enums.NotificationType
import com.perevodchik.enums.OrdersStatus
import com.perevodchik.repository.*
import com.perevodchik.utils.DateTimeUtil
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.rules.SecurityRule
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

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Get("/all")
    fun availableOrders(authentication: Authentication, @QueryValue(value = "page") page: Int, @QueryValue(value = "limit") limit: Int): List<AvailableOrderPreview> {
        return ordersService.getAvailableOrders(page, limit)
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Get("/{id}")
    fun get(@PathVariable id: Int, authentication: Authentication): HttpResponse<*> {
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
        val newOrder = ordersService.createOrder(order, id) ?: return HttpResponse.badRequest()
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
    fun updateOrder(@Body order: Order, authentication: Authentication): HttpResponse<Order> {
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
            1 -> {
                if(order.clientId == currentUser) {
                    val isUpdate = ordersService.updateOrderStatus(payload.orderId, payload.status, payload.masterId)
                    if(isUpdate)
                        notificationsService.createNotification(
                                Notification(
                                        id = 0,
                                        userId = currentUser,
                                        secondUserId = payload.masterId,
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
                if(currentUser == order.clientId || currentUser == order.masterId) {
                    if(order.clientId == currentUser)
                        return HttpResponse.ok(ordersService.updateOrderStatus(payload.orderId, payload.status, payload.masterId))
                } else {
                    return HttpResponse.badRequest()
                }
            }
            // send private order to master
            3 -> {
                if(currentUser == order.clientId || currentUser == order.masterId) {
                    if(order.clientId == currentUser)
                        return HttpResponse.ok(ordersService.updateOrderStatus(payload.orderId, payload.status, payload.masterId))
                } else {
                    return HttpResponse.badRequest()
                }
            }
            // cancel order
            4 -> {
                if(order.status != 4) {
                    val isUpdate = ordersService.updateOrderStatus(payload.orderId, payload.status, payload.masterId)
                    if(isUpdate) {
                        notificationsService.createNotification(
                                Notification(
                                        id = 0,
                                        userId = currentUser,
                                        secondUserId = payload.masterId,
                                        orderId = payload.orderId,
                                        notificationType = if(currentRole == 0) NotificationType.ORDER_CANCELLED_BY_CLIENT.value else NotificationType.ORDER_CANCELLED_BY_MASTER.value,
                                        createdAt = DateTimeUtil.timestamp()
                                )
                        )
                    }
                    return HttpResponse.ok(isUpdate)
                }
                return HttpResponse.badRequest()
            }
        }
        return HttpResponse.badRequest()
    }
}

