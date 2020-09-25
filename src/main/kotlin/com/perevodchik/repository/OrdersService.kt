package com.perevodchik.repository

import com.perevodchik.domain.*

interface OrdersService {
    fun isClientRecordedToMaster(firstUserId: Int, secondUserId: Int): Boolean
    fun getAvailableOrders(page: Int, limit: Int): List<AvailableOrderPreview>
    fun getUserOrders(userId: Int, role: Int): List<OrderPreview>
    fun createOrder(order: OrderShort, clientId: Int): Order?
    fun updateOrder(order: Order): Order?
    fun getOrderById(id: Int, sentenceService: SentenceService): OrderFull?
    fun deleteOrder(order: Order)
    fun addOrderService(orderId: Int, serviceId: Int)
    fun updateOrderStatus(orderId: Int, status: Int, masterId: Int?): Boolean
    fun getShortOrderById(orderId: Int): OrderExtraShort?
}