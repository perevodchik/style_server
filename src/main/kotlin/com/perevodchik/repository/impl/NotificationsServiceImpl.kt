package com.perevodchik.repository.impl

import com.perevodchik.domain.Notification
import com.perevodchik.repository.NotificationsService
import com.perevodchik.utils.DateTimeUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationsServiceImpl: NotificationsService {

    @Inject
    lateinit var pool: io.reactiverse.reactivex.pgclient.PgPool

    override fun createNotification(notification: Notification): Notification? {
        val r = pool
                .rxQuery("INSERT INTO notifications " +
                        "(user_id, second_user_id, order_id, notification_type, created_at) VALUES " +
                        "(${notification.userId}, ${notification.secondUserId}, ${notification.orderId}, ${notification.notificationType}, ${notification.createdAt ?: DateTimeUtil.timestamp()})" +
                        " RETURNING notifications.id;")
                .blockingGet()
        return null
    }

    override fun getNotifications(userId: Int, page: Int, limit: Int): List<Notification> {
        val notifications = mutableListOf<Notification>()

        val r = pool
                .rxQuery("SELECT * FROM notifications WHERE user_id = $userId ORDER BY id DESC OFFSET $page LIMIT $limit;")
                .blockingGet()
        val i = r.iterator()
        while(i.hasNext()) {
            val r = i.next()
        }
        return notifications
    }

    override fun hasUnreadNotifications(userId: Int): Int {
        val r = pool
                .rxQuery("SELECT COUNT(id) as unread_count FROM notifications WHERE user_id = $userId;")
                .blockingGet()
        return r.iterator().next().getInteger("unread_count") ?: 0
    }

    override fun markNotifications(userId: Int, notificationId: Int): Boolean {
        val r = pool.rxQuery("UPDATE notifications SET is_dirty = TRUE where user_id = $userId AND is_dirty = FALSE AND id <= $notificationId;").blockingGet()
        return r.rowCount() > 0
    }

    override fun markNotification(userId: Int, notificationId: Int): Boolean {
        val r = pool.rxQuery("UPDATE notifications SET is_dirty = TRUE where user_id = $userId AND is_dirty = FALSE AND id = $notificationId;").blockingGet()
        return r.rowCount() > 0
    }
}