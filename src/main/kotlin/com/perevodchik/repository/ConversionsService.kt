package com.perevodchik.repository

import com.perevodchik.domain.*
import com.perevodchik.utils.DateTimeUtil
import com.perevodchik.utils.FileUtils
import io.micronaut.http.multipart.StreamingFileUpload
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

interface ConversionsService {
    fun createConversion(clientId: Int, masterId: Int): Optional<ConversionPreview>
    fun getConversions(userId: Int, label: Boolean): List<ConversionPreview>
    fun getConversion(conversionId: Int): Optional<ConversionPreview>
    fun getConversion(clientId: Int, masterId: Int): Optional<ConversionPreview>
    fun sendMessage(message: Message): Optional<Message>
    fun getMessagesByConversion(conversionId: Int, userId: Int, page: Optional<Int>, limit: Optional<Int>): JsonObject
    fun read(conversionId: Int, userId: Int, messageId: Int)
    fun upload(name: String, upload: StreamingFileUpload): String
}

@Singleton
class ConversionsServiceImpl: ConversionsService {
    @Inject
    lateinit var pool: io.reactiverse.reactivex.pgclient.PgPool

    override fun createConversion(clientId: Int, masterId: Int): Optional<ConversionPreview> {
        val conversion = getConversion(clientId, masterId)
        if(conversion.isEmpty) {
            val r = pool.rxQuery("INSERT INTO conversions (client_id, master_id) VALUES ($clientId, $masterId) RETURNING conversions.id;").blockingGet()
            val i = r.iterator()
            if(i.hasNext()) {
                val row = i.next()
                pool.rxQuery("INSERT INTO conversion_data (user_id, conversion_id) VALUES ($clientId, ${row.getInteger("id")});").blockingGet()
                pool.rxQuery("INSERT INTO conversion_data (user_id, conversion_id) VALUES ($masterId, ${row.getInteger("id")});").blockingGet()
                return getConversion(i.next().getInteger("id"))
            }
        }
        return Optional.empty()
    }

    override fun getConversions(userId: Int, label: Boolean): List<ConversionPreview> {
        val conversions = mutableListOf<ConversionPreview>()
        val r = pool.rxQuery("SELECT c.id, m.id as message_id, m.sender_id, m.message, m.has_media, m.created_at, u.id as user_id, u.name, u.surname, u.avatar, d.last_read_message_id " +
                "FROM conversions c " +
                "LEFT JOIN conversion_data d ON d.user_id = $userId" +
                "LEFT JOIN messages m ON m.id = c.last_message_id " +
                "LEFT JOIN users u ON u.id = ${if(!label) "c.master_id" else "c.client_id"} " +
                "WHERE ${if(label) "c.master_id" else "c.client_id"} = $userId ORDER BY message_id DESC;").blockingGet()
        val i = r.iterator()
        while(i.hasNext()) {
            val row = i.next()
            val userShort = if(row.getInteger("user_id") != null) UserShort(
                    id = row.getInteger("user_id"),
                    name = row.getString("name") ?: "",
                    surname = row.getString("surname") ?: "",
                    avatar = row.getString("avatar") ?: ""
            ) else null
            val message = if(row.getInteger("message_id") != null) Message(
                    id = row.getInteger("message_id"),
                    conversionId = row.getInteger("id"),
                    senderId = row.getInteger("sender_id"),
                    message = row.getString("message") ?: "",
                    hasMedia = row.getBoolean("has_media") ?: false,
                    createdAt = row.getString("created_at") ?: DateTimeUtil.timestamp(),
                    receiverId = -1
            ) else null
            val conversionPreview = ConversionPreview(
                    id = row.getInteger("id"),
                    lastReadMessageId = row.getInteger("last_read_message_id") ?: -1,
                    user = userShort,
                    message = message
            )
            conversions.add(conversionPreview)
        }
        return conversions
    }

    override fun getConversion(conversionId: Int): Optional<ConversionPreview> {
        val r = pool.rxQuery("SELECT c.id, m.id as message_id, m.sender_id as sender, m.message, m.has_media, m.created_at, u.name, u.surname, u.avatar\n" +
                "FROM conversions c " +
                "RIGHT JOIN messages m ON m.id = c.last_message_id " +
                "RIGHT JOIN users u ON u.id = c.master_id " +
                "WHERE c.id = $conversionId;").blockingGet()
        val i = r.iterator()
        if(i.hasNext()) {
            val row = i.next()
            val conversionPreview = ConversionPreview(
                    id = row.getInteger("id"),
                    lastReadMessageId = -1,
                    user = UserShort(
                            id = row.getInteger("sender"),
                            name = row.getString("name") ?: "",
                            surname = row.getString("surname") ?: "",
                            avatar = row.getString("avatar") ?: ""
                    ),
                    message = Message(
                            id = row.getInteger("message_id"),
                            conversionId = row.getInteger("id"),
                            senderId = row.getInteger("sender"),
                            message = row.getString("message") ?: "",
                            hasMedia = row.getBoolean("has_media") ?: false,
                            createdAt = row.getString("created_at") ?: DateTimeUtil.timestamp(),
                            receiverId = -1
                    )
            )
            return Optional.of(conversionPreview)
        }
        return Optional.empty()
    }

    override fun getConversion(clientId: Int, masterId: Int): Optional<ConversionPreview> {
        val r = pool.rxQuery("SELECT c.id, m.id as message_id, m.sender_id as sender, m.message, m.has_media, m.created_at, u.name, u.surname, u.avatar\n" +
                "FROM conversions c " +
                "RIGHT JOIN messages m ON m.id = c.last_message_id " +
                "RIGHT JOIN users u ON u.id = c.master_id " +
                "WHERE c.client_id = $clientId AND c.master_id = $masterId;").blockingGet()
        val i = r.iterator()
        if(i.hasNext()) {
            val row = i.next()
            val conversionPreview = ConversionPreview(
                    id = row.getInteger("id"),
                    lastReadMessageId = -1,
                    user = UserShort(
                            id = row.getInteger("sender_id"),
                            name = row.getString("name") ?: "",
                            surname = row.getString("surname") ?: "",
                            avatar = row.getString("avatar") ?: ""
                    ),
                    message = Message(
                            id = row.getInteger("message_id"),
                            conversionId = row.getInteger("id"),
                            senderId = row.getInteger("sender_id"),
                            message = row.getString("message") ?: "",
                            hasMedia = row.getBoolean("has_media") ?: false,
                            createdAt = row.getString("created_at") ?: DateTimeUtil.timestamp(),
                            receiverId = -1
                    )
            )
            return Optional.of(conversionPreview)
        }
        return Optional.empty()
    }

    override fun sendMessage(message: Message): Optional<Message> {
        val r = pool.rxQuery("INSERT INTO messages " +
                "(sender_id, conversion_id, has_media, message, created_at) VALUES " +
                "(${message.senderId}, ${message.conversionId}, ${message.hasMedia}, '${message.message}', '${message.createdAt}') " +
                "RETURNING messages.id;").blockingGet()
        val i = r.iterator()
        if(i.hasNext()) {
            message.id = i.next().getInteger("id")
            val r0 = pool.rxQuery("UPDATE conversions SET last_message_id = ${message.id} WHERE id = ${message.conversionId};").blockingGet()
            println("conversions update: ${r0.rowCount()}")
            return Optional.of(message)
        }
        return Optional.empty()
    }

    override fun getMessagesByConversion(conversionId: Int, userId: Int, page: Optional<Int>, limit: Optional<Int>): JsonObject {
        val messages = mutableListOf<Message>()
        val messagesJsonArray = JsonArray()
        val q = "SELECT * FROM messages WHERE conversion_id = $conversionId ORDER BY id DESC OFFSET ${page.orElse(0) * limit.orElse(0)} LIMIT ${limit.orElse(100)};"
        println(q)
        val r = pool.rxQuery(q).blockingGet()
        val i = r.iterator()
        while(i.hasNext()) {
            val row = i.next()
            val message = Message(
                    id = row.getInteger("id"),
                    conversionId = row.getInteger("conversion_id"),
                    senderId = row.getInteger("sender_id"),
                    message = row.getString("message") ?: "",
                    hasMedia = row.getBoolean("has_media") ?: false,
                    createdAt = row.getString("created_at") ?: DateTimeUtil.timestamp(),
                    receiverId = -1
            )
            messages.add(message)
        }
        val jsonObject = JsonObject()
        messagesJsonArray.list.addAll(messages)
        jsonObject.put("messages", messagesJsonArray)
//        return messages
        return jsonObject
    }

    override fun read(conversionId: Int, userId: Int, messageId: Int) {
        pool.rxQuery("UPDATE conversion_data SET last_read_message_id = $messageId WHERE user_id = $userId AND conversion_id = $conversionId;").blockingGet()
    }

    override fun upload(name: String, upload: StreamingFileUpload): String {
        return FileUtils().uploadFile(upload, name)
    }

}