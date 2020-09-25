package com.perevodchik.repository

import com.perevodchik.domain.Notification

interface NotificationsService {
    fun createNotification(notification: Notification): Notification?
    fun getNotifications(userId: Int, page: Int, limit: Int): List<Notification>
    fun hasUnreadNotifications(userId: Int): Int
    fun markNotifications(userId: Int, notificationId: Int): Boolean
    fun markNotification(userId: Int, notificationId: Int): Boolean
}