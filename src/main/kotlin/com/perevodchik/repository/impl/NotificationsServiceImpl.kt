package com.perevodchik.repository.impl

import com.perevodchik.domain.Notification
import com.perevodchik.domain.NotificationFull
import com.perevodchik.domain.OrderName
import com.perevodchik.domain.UserShort
import com.perevodchik.repository.NotificationsService
import com.perevodchik.utils.DateTimeUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationsServiceImpl: NotificationsService {

    @Inject
    lateinit var pool: io.reactiverse.reactivex.pgclient.PgPool

    override fun createNotification(notification: Notification): Notification? {
        println("$notification")
        val q = "INSERT INTO notifications " +
                "(user_id, second_user_id, order_id, notification_type, created_at) VALUES " +
                "(${notification.userId}, ${notification.secondUserId}, ${notification.orderId}, ${notification.notificationType}, '${notification.createdAt ?: DateTimeUtil.timestamp()}')" +
                " RETURNING notifications.id;"
        pool.rxQuery(q).blockingGet()
        print("notification created")
        return null
    }

    override fun getNotifications(userId: Int): List<NotificationFull> {
        val notifications = mutableListOf<NotificationFull>()
        val result = pool
                .rxQuery("SELECT n.id, n.notification_type, n.created_at, n.is_dirty, " +
                        "u.id as user_id, u.name as user_name, u.surname as user_surname, " +
                        "s.id as second_user_id, s.name as second_user_name, s.surname as second_user_surname, " +
                        "o.id as order_id, o.name as order_name " +
                        "FROM notifications n " +
                        "LEFT JOIN users u ON u.id = n.user_id " +
                        "LEFT JOIN users s ON s.id = n.second_user_id " +
                        "LEFT JOIN orders o ON o.id = n.order_id " +
                        "WHERE user_id = $userId " +
                        "ORDER BY id DESC LIMIT 100;")
                .blockingGet()
        val i = result.iterator()
        while(i.hasNext()) {
            val r = i.next()
            var userData: UserShort? = null
            if(r.getInteger("user_id") != null) {
                userData = UserShort(
                        id = r.getInteger("user_id"),
                        name = r.getString("user_name"),
                        surname = r.getString("user_surname"),
                        avatar = ""
                )
            }
            var secondUserData: UserShort? = null
            if(r.getInteger("second_user_id") != null) {
                secondUserData = UserShort(
                        id = r.getInteger("second_user_id"),
                        name = r.getString("second_user_name"),
                        surname = r.getString("second_user_surname"),
                        avatar = ""
                )
            }
            var order: OrderName? = null
            if(r.getInteger("order_id") != null) {
                order = OrderName(
                        id = r.getInteger("order_id"),
                        name = r.getString("order_name")
                )
            }
            val notificationFull = NotificationFull(
                    id = r.getInteger("id"),
                    user = userData,
                    secondUser = secondUserData,
                    order = order,
                    notificationType = r.getInteger("notification_type"),
                    isDirty = r.getBoolean("is_dirty"),
                    createdAt = r.getString("created_at")
            )
            notifications.add(notificationFull)
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