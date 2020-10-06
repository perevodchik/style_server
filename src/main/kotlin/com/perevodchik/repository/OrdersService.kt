package com.perevodchik.repository

import com.perevodchik.domain.*
import io.micronaut.http.multipart.StreamingFileUpload

interface OrdersService {
    fun isClientRecordedToMaster(firstUserId: Int, secondUserId: Int): Boolean
    fun getAvailableOrders(page: Int, limit: Int, cities: String, services: String, withPrice: Boolean, withCity: Boolean): List<AvailableOrderPreview>
    fun getUserOrders(userId: Int, role: Int): List<OrderPreview>
    fun createOrder(order: OrderShort, clientId: Int): Order?
    fun updateOrder(order: Order): Order?
    fun getOrderById(id: Int, sentenceService: SentenceService): OrderFull?
    fun deleteOrder(order: Order)
    fun addOrderService(orderId: Int, serviceId: Int)
    fun updateOrderStatus(orderId: Int, status: Int, masterId: Int?): Boolean
    fun getShortOrderById(orderId: Int): OrderExtraShort?
    fun upload(orderId: Int, fileName: String, upload: StreamingFileUpload): String
    fun addExistingImage(existingImage: ExistingImage): String
    fun isPhotoUseInOrder(image: String): Boolean
}