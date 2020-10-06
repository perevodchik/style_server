package com.perevodchik.domain

data class Notification(
        var id: Int,
        var userId: Int?,
        var secondUserId: Int?,
        var orderId: Int?,
        var notificationType: Int,
        var createdAt: String?
)

data class NotificationFull(
        var id: Int,
        var user: UserShort?,
        var secondUser: UserShort?,
        var order: OrderName?,
        var notificationType: Int,
        var isDirty: Boolean,
        var createdAt: String?
)