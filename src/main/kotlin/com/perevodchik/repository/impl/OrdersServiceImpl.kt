package com.perevodchik.repository.impl

import com.perevodchik.domain.*
import com.perevodchik.repository.OrdersService
import com.perevodchik.repository.SentenceService
import com.perevodchik.utils.DateTimeUtil
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrdersServiceImpl: OrdersService {

    @Inject
    lateinit var pool: io.reactiverse.reactivex.pgclient.PgPool

    override fun isClientRecordedToMaster(firstUserId: Int, secondUserId: Int): Boolean {
        val r = pool.rxQuery("SELECT COUNT(id) as count FROM orders WHERE ((master_id = $secondUserId AND client_id = $firstUserId) OR (client_id = $secondUserId AND master_id = $firstUserId)) AND (status = 3 OR status = 2);").blockingGet()
        return r.iterator().hasNext()
    }

    override fun getAvailableOrders(page: Int, limit: Int): List<AvailableOrderPreview> {
        val r = pool.rxQuery("SELECT orders.id, orders.name, orders.description, orders.price, orders.created_at FROM orders WHERE status = 0 AND orders.master_id IS NULL AND orders.is_private = FALSE ORDER BY orders.id DESC OFFSET $page LIMIT $limit;").blockingGet()
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
                        "client_id, master_id, sketch_id, sketch_data_id, status, price, name, description, is_private" +
                        ") VALUES (" +
                        "${clientId}, ${order.masterId}, ${order.sketchId}, $sketchDataId, ${order.status}, ${order.price}, '${order.name}', '${order.description}', ${order.isPrivate}" +
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
        val r = pool.rxQuery("SELECT orders.id, orders.status, users.id as client_id, users.name as client_name, users.surname as client_surname, users.avatar as client_avatar, orders.name, orders.description, orders.price, master.id as master_id, master.name as master_name, master.surname as master_surname, master.avatar as master_avatar, positions.id as position_id, positions.name as position_name, styles.id as style_id, styles.name as style_name, data.id as sketch_data_id, data.width, data.height, data.is_colored,orders.is_private, orders.created_at as created FROM orders LEFT JOIN sketch_data as data ON data.id = orders.sketch_data_id LEFT JOIN positions ON data.position_id = positions.id LEFT JOIN styles ON data.style_id = styles.id JOIN users ON users.id = orders.client_id LEFT JOIN users as master ON master.id = orders.master_id " +
                "WHERE orders.id = $id;").blockingGet()
        val i0 = r.iterator()
        if(i0.hasNext()) {
            val row = i0.next()
            val position: Position?
            val style: Style?
            var sketchData: SketchDataFull? = null
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
            val clientData = UserShort(
                    id = row.getInteger("client_id"),
                    name = row.getString("client_name"),
                    surname = row.getString("client_surname"),
                    avatar = row.getString("client_avatar")
            )
            var masterData: UserShort? = null
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
                    isPrivate = row.getBoolean("is_private"),
                    sketchData = sketchData,
                    client = clientData,
                    master = masterData,
                    created = row.getString("created") ?: DateTimeUtil.timestamp(),
                    sentences = sentences,
                    services = mutableListOf()
            )
            val serviceIds = mutableListOf<Int>()
            val r1 = pool.rxQuery("SELECT service_id FROM order_services WHERE order_id = ${order.id}").blockingGet()
            val i1 = r1.iterator()
            while(i1.hasNext()) {
                serviceIds.add(i1.next().getInteger("service_id"))
            }
            if(serviceIds.isNotEmpty()) {
                val q = "SELECT services.name FROM services WHERE id IN (${serviceIds.joinToString(", ")});"
                println(q)
                val r2 = pool.rxQuery(q).blockingGet()
                val i2 = r2.iterator()
                while(i2.hasNext()) {
                    order.services.add(i2.next().getString("name"))
                }
            }
            println("$order")
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
        val additionalArgs = if(masterId == null) "" else ", master_id = $masterId"
        val r = pool.rxQuery("UPDATE orders SET status = $status $additionalArgs WHERE id = $orderId;").blockingGet()
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
}