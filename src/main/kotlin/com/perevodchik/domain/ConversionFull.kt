package com.perevodchik.domain

import com.perevodchik.utils.DateTimeUtil
import io.vertx.core.json.JsonObject

data class ConversionFull (
        var id: Int,
        var firstUserId: Int,
        var secondUserId: Int,
        var lastMessageId: Int
)

data class ConversionPreview(
        var id: Int,
        var lastReadMessageId: Int,
        var user: UserShort?,
        var message: Message?
)

data class Message(
        var id: Int,
        var conversionId: Int,
        var senderId: Int,
        var receiverId: Int,
        var message: String,
        var hasMedia: Boolean,
        var createdAt: String = DateTimeUtil.timestamp()
) {
    fun toJson(): JsonObject {
    val json = JsonObject()
        json.put("id", id)
        json.put("conversionId", conversionId)
        json.put("senderId", senderId)
        json.put("receiverId", receiverId)
        json.put("message", message)
        json.put("hasMedia", hasMedia)
        json.put("createdAt", createdAt)
    return json
    }
}