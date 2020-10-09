package com.perevodchik.repository.impl

import com.perevodchik.domain.*
import com.perevodchik.repository.OrdersService
import com.perevodchik.repository.SentenceService
import com.perevodchik.utils.DateTimeUtil
import com.perevodchik.utils.FileUtils
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.Consumes
import io.micronaut.http.annotation.Post
import io.micronaut.http.annotation.Produces
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.security.annotation.Secured
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.rules.SecurityRule
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrdersServiceImpl: OrdersService {

    @Inject
    lateinit var pool: io.reactiverse.reactivex.pgclient.PgPool

    override fun isClientRecordedToMaster(firstUserId: Int, secondUserId: Int): Boolean {
        println("SELECT COUNT(id) as count FROM orders WHERE ((master_id = $secondUserId AND client_id = $firstUserId) OR (client_id = $secondUserId AND master_id = $firstUserId)) AND status = 2;")
        val r = pool.rxQuery("SELECT COUNT(id) as count FROM orders WHERE ((master_id = $secondUserId AND client_id = $firstUserId) OR (client_id = $secondUserId AND master_id = $firstUserId)) AND status = 2;").blockingGet()
        val c = r.iterator().next().getInteger("count")
        println("records count for $firstUserId AND $secondUserId = $c")
        return (c > 0)
    }

    override fun getAvailableOrders(page: Int, limit: Int, cities: String, services: String, withPrice: Boolean, withCity: Boolean): List<AvailableOrderPreview> {
        val q = "SELECT o.id, o.name, o.description, o.price, o.created_at FROM orders o " +
                "WHERE status = 0 " +
                "AND o.master_id IS NULL " +
                "AND o.is_private = FALSE " +
                (if(cities.isNotEmpty()) "AND o.city_id IN ($cities) " else "") +
                (if(services.isNotEmpty()) {
                    var servicesString = ""
                    for(s in services.split(","))
                        servicesString += "'$s',"
                    if(servicesString.endsWith(","))
                        servicesString = servicesString.substring(0, servicesString.length - 1)
                    "AND (SELECT ARRAY[array_agg(distinct order_services.service_id)] as services FROM orders LEFT JOIN order_services ON o.id = order_services.order_id) && ARRAY[$servicesString]::int[] "
                } else "") +
                (if(withPrice) "AND o.price IS NOT NULL AND o.price > 0 " else "") +
                (if(withCity) "AND o.city_id IS NOT NULL " else "") +
                "ORDER BY o.id DESC OFFSET $page LIMIT $limit;"
        println(q)
        val r = pool.rxQuery(q).blockingGet()
        val i = r.iterator()
        val orders = mutableListOf<AvailableOrderPreview>()
        while(i.hasNext()) {
            val row = i.next()
            val order = AvailableOrderPreview(
                    id = row.getInteger("id"),
                    price = row.getInteger("price"),
                    name = row.getString("name"),
                    description = row.getString("description"),
                    created = row.getString("created_at") ?: DateTimeUtil.timestamp()
            )
            orders.add(order)
        }
        return orders
    }

    override fun getUserOrders(userId: Int, role: Int): List<OrderPreview> {
        val r = pool.rxQuery("SELECT orders.id, orders.name, orders.price, orders.status, COUNT(sentences.id) as sentences_count FROM orders LEFT JOIN sentences ON sentences.order_id = orders.id WHERE orders.${if(role == 0) "client_id" else "master_id"} = $userId GROUP BY orders.id ORDER BY id DESC LIMIT 30").blockingGet()
        val i = r.iterator()
        val orders = mutableListOf<OrderPreview>()
        while(i.hasNext()) {
            val row = i.next()
            val order = OrderPreview(
                    row.getInteger("id"),
                    row.getString("name"),
                    row.getInteger("price"),
                    row.getInteger("status"),
                    row.getInteger("sentences_count")
            )
            orders.add(order)
        }
        return orders
    }

    override fun createOrder(order: OrderShort, clientId: Int): Order? {
        var sketchDataId: Int? = null

        println("create [ $order ]")

        if(order.sketchData != null) {
            val sketchDataResult = pool.rxQuery("INSERT INTO sketch_data (position_id, style_id, width, height, is_colored) VALUES (${order.sketchData?.positionId}, ${order.sketchData?.styleId}, ${order.sketchData?.width}, ${order.sketchData?.height}, ${order.sketchData?.isColored}) RETURNING sketch_data.id;").blockingGet()
            val i = sketchDataResult.iterator()
            if(i.hasNext()) {
                sketchDataId = i.next().getInteger("id")
            }
        }

        println("sketchDataId $sketchDataId")

        val r = pool.rxQuery(
                "INSERT INTO orders (" +
                        "client_id, master_id, sketch_id, city_id, sketch_data_id, status, price, name, description, is_private" +
                        ") VALUES (" +
                        "${clientId}, ${order.masterId}, ${order.sketchId}, ${order.cityId}, $sketchDataId, ${order.status}, ${order.price}, '${order.name}', '${order.description}', ${order.isPrivate}" +
                        ") RETURNING orders.id;"
        ).blockingGet()
        val i = r.iterator()
        if(i.hasNext()) {
            val row = i.next()
            val newOrder = Order(
                        id = row.getInteger("id"),
                        clientId = clientId,
                        masterId = order.masterId,
                        sketchDataId = null,
                        status = order.status,
                        price = order.price,
                        name = order.name,
                        description = order.description,
                        isPrivate = order.isPrivate,
                        photos = mutableListOf(),
                        services = order.services,
                        sketchData = order.sketchData
                    )
            println("newOrder $newOrder")
            println(order.services)
            for(serviceId in order.services) {
                println("create order service $serviceId")
                val rr = pool.rxQuery("INSERT INTO order_services (service_id, order_id) VALUES ($serviceId, ${newOrder.id});").blockingGet()
                print(rr.rowCount())
            }
            return newOrder
        }
        return null
    }

    override fun updateOrder(order: Order): Order? {
        pool.rxQuery(
                "UPDATE orders SET master_id = ${order.masterId}, status = ${order.status}, price = ${order.price} WHERE id = ${order.id};"
        ).blockingGet()
        return order
    }

    override fun getOrderById(id: Int, sentenceService: SentenceService): OrderFull? {
        val q = "SELECT orders.id, orders.status, " +
                "users.id as client_id, users.name as client_name, users.surname as client_surname, users.avatar as client_avatar, " +
                "orders.name, orders.description, orders.price, " +
                "cities.id as city_id, cities.name as city_name, " +
                "master.id as master_id, master.name as master_name, master.surname as master_surname, master.avatar as master_avatar, " +
                "positions.id as position_id, positions.name as position_name, " +
                "styles.id as style_id, styles.name as style_name, data.id as sketch_data_id, " +
                "data.width, data.height, data.is_colored, " +
                "string_agg(distinct order_photos.image, ',') as photos, " +
                "orders.is_private, orders.created_at as created, orders.client_comment_id, orders.master_comment_id " +
                "FROM orders " +
                "LEFT JOIN sketch_data as data ON data.id = orders.sketch_data_id " +
                "LEFT JOIN positions ON data.position_id = positions.id " +
                "LEFT JOIN styles ON data.style_id = styles.id " +
                "LEFT JOIN cities ON orders.city_id = cities.id " +
                "LEFT JOIN order_photos ON orders.id = order_photos.order_id " +
                "JOIN users ON users.id = orders.client_id " +
                "LEFT JOIN users as master ON master.id = orders.master_id " +
                "WHERE orders.id = $id GROUP BY orders.id, users.id, cities.id, styles.id, positions.id, master.id, data.id;"
        val r = pool.rxQuery(q).blockingGet()
        val i0 = r.iterator()
        if(i0.hasNext()) {
            val row = i0.next()
            val position: Position?
            val style: Style?
            var city: City? = null
            var sketchData: SketchDataFull? = null
            var masterData: UserShort? = null
            val clientData = UserShort(
                    id = row.getInteger("client_id"),
                    name = row.getString("client_name"),
                    surname = row.getString("client_surname"),
                    avatar = row.getString("client_avatar")
            )
            if(row.getInteger("sketch_data_id") != null) {
                position = Position(
                        row.getInteger("position_id"),
                        row.getString("position_name")
                )
                style = Style(
                        row.getInteger("style_id"),
                        row.getString("style_name")
                )
                sketchData = SketchDataFull(
                        id = row.getInteger("sketch_data_id"),
                        position = position,
                        style = style,
                        width = row.getInteger("width"),
                        height = row.getInteger("height"),
                        isColored = row.getBoolean("is_colored")
                )
            }
            if(row.getInteger("city_id") != null)
                city = City(
                        id = row.getInteger("city_id"),
                        name = row.getString("city_name")
                )
            if(row.getInteger("master_id") != null) {
                masterData = UserShort(
                        id = row.getInteger("master_id"),
                        name = row.getString("master_name"),
                        surname = row.getString("master_surname"),
                        avatar = row.getString("master_avatar")
                )
            }
            val sentences = sentenceService.getSentencesByOrder(id)
            val order = OrderFull(
                    id = row.getInteger("id"),
                    status = row.getInteger("status"),
                    price = row.getInteger("price"),
                    name = row.getString("name"),
                    description = row.getString("description"),
                    photos = row.getString("photos") ?: "",
                    isPrivate = row.getBoolean("is_private"),
                    sketchData = sketchData,
                    client = clientData,
                    master = masterData,
                    city = city,
                    created = row.getString("created") ?: DateTimeUtil.timestamp(),
                    sentences = sentences,
                    services = mutableListOf(),
                    clientComment = null,
                    masterComment = null
            )
            val serviceIds = mutableListOf<Int>()
            val r1 = pool.rxQuery("SELECT service_id FROM order_services WHERE order_id = ${order.id}").blockingGet()
            val i1 = r1.iterator()
            while(i1.hasNext()) {
                serviceIds.add(i1.next().getInteger("service_id"))
            }
            if(serviceIds.isNotEmpty()) {
                val r2 = pool.rxQuery("SELECT services.name FROM services WHERE id IN (${serviceIds.joinToString(", ")});").blockingGet()
                val i2 = r2.iterator()
                while(i2.hasNext()) {
                    order.services.add(i2.next().getString("name"))
                }
            }
            if(row.getInteger("client_comment_id") != null) {
                val r2 = pool
                        .rxQuery("SELECT comments.id as comment_id, comments.target_id, comments.commentator_id, " +
                                "users.name, users.surname, users.avatar, " +
                                "comments.rate, comments.message, comments.created_at " +
                                "FROM comments " +
                                "INNER JOIN users ON users.id = comments.commentator_id " +
                                "WHERE comments.id = ${row.getInteger("client_comment_id")};")
                        .blockingGet()
                val i = r2.iterator()
                if(i.hasNext()) {
                    val row1 = i.next()
                    val comment = CommentFull(
                            id = row1.getInteger("comment_id"),
                            targetId = row1.getInteger("target_id"),
                            commentatorId = row1.getInteger("commentator_id"),
                            commentatorName = row1.getString("name"),
                            commentatorSurname = row1.getString("surname"),
                            commentatorAvatar = row1.getString("avatar"),
                            message = row1.getString("message"),
                            rate = row1.getDouble("rate"),
                            createdAt = row1.getString("createdAt") ?: DateTimeUtil.timestamp()
                    )
                    order.clientComment = comment
                }
            }
            if(row.getInteger("master_comment_id") != null) {
                val r2 = pool
                        .rxQuery("SELECT comments.id as comment_id, comments.target_id, comments.commentator_id, " +
                                "users.name, users.surname, users.avatar, " +
                                "comments.rate, comments.message, comments.created_at " +
                                "FROM comments " +
                                "INNER JOIN users ON users.id = comments.commentator_id " +
                                "WHERE comments.id = ${row.getInteger("master_comment_id")};")
                        .blockingGet()
                val i = r2.iterator()
                if(i.hasNext()) {
                    val row1 = i.next()
                    val comment = CommentFull(
                            id = row1.getInteger("comment_id"),
                            targetId = row1.getInteger("target_id"),
                            commentatorId = row1.getInteger("commentator_id"),
                            commentatorName = row1.getString("name"),
                            commentatorSurname = row1.getString("surname"),
                            commentatorAvatar = row1.getString("avatar"),
                            message = row1.getString("message"),
                            rate = row1.getDouble("rate"),
                            createdAt = row1.getString("createdAt") ?: DateTimeUtil.timestamp()
                    )
                    order.masterComment = comment
                }
            }
            return order
        }
        return null
    }

    override fun deleteOrder(order: Order) {
        pool.query("DELETE FROM orders WHERE id = ${order.id}") {}
    }

    override fun addOrderService(orderId: Int, serviceId: Int) {
        pool.rxQuery("INSERT INTO orders_services (order_id, service_id) VALUES ($orderId, $serviceId);")
    }

    override fun updateOrderStatus(orderId: Int, status: Int, masterId: Int?): Boolean {
        println("updateOrderStatus staer")
        val additionalArgs = if(masterId == null) "" else ", master_id = $masterId"
        val r = pool.rxQuery("UPDATE orders SET status = $status $additionalArgs WHERE id = $orderId;").blockingGet()
        println("updateOrderStatus end")
        return r.rowCount() > 0
    }

    override fun getShortOrderById(orderId: Int): OrderExtraShort? {
        val r = pool.rxQuery("SELECT id, master_id, client_id, status, is_private FROM orders WHERE id = $orderId;").blockingGet()
        val i = r.iterator()
        if(i.hasNext()) {
            val row = i.next()
            return OrderExtraShort(
                    orderId = row.getInteger("id"),
                    masterId = row.getInteger("master_id"),
                    clientId = row.getInteger("client_id"),
                    status = row.getInteger("status"),
                    isPrivate = row.getBoolean("is_private")
            )
        }
        return null
    }

    override fun upload(orderId: Int, fileName: String, upload: StreamingFileUpload): String {
        try {
            val uploadResult = FileUtils().uploadFile(upload, fileName)
            if(uploadResult.isNotEmpty() && uploadResult.isNotBlank()) {
                val r = pool.rxQuery("INSERT INTO order_photos (image, order_id) VALUES ('$uploadResult', $orderId);").blockingGet()
                if(r.rowCount() == 0) {
                    FileUtils().deleteFile("static/$uploadResult")
                    return ""
                }
                return uploadResult
            }
            return ""
        } catch(ex: Exception) {
            println(ex.localizedMessage)
            return ""
        }
    }

    override fun addExistingImage(existingImage: ExistingImage): String {
        val q = "INSERT INTO order_photos (image, order_id) VALUES ('${existingImage.image}', ${existingImage.orderId});"
        println(q)
        val r = pool.rxQuery(q).blockingGet()
        return if(r.rowCount() > 0)
            existingImage.image
        else return ""
    }

    override fun isPhotoUseInOrder(image: String): Boolean {
        val r = pool.rxQuery("SELECT COUNT(id) as count FROM order_photos WHERE image = '$image';").blockingGet()
        val i = r.iterator()
        if(i.hasNext()) {
            val row = i.next()
            val count = row.getInteger("count")
            return count > 0
        }
        return false
    }
}