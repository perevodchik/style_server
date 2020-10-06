package com.perevodchik.repository

import com.perevodchik.domain.Notification
import com.perevodchik.domain.NotificationFull

interface NotificationsService {
    fun createNotification(notification: Notification): Notification?
    fun getNotifications(userId: Int): List<NotificationFull>
    fun hasUnreadNotifications(userId: Int): Int
    fun markNotifications(userId: Int, notificationId: Int): Boolean
    fun markNotification(userId: Int, notificationId: Int): Boolean
}