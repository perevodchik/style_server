package com.perevodchik.domain

import com.perevodchik.utils.DateTimeUtil

data class OrderFull(
        var id: Int,
        var status: Int = 0,
        var price: Int = 0,
        var name: String,
        var description: String,
        var photos: String,
        var isPrivate: Boolean = false,
        var client: UserShort,
        var master: UserShort?,
        var city: City?,
        val sketchData: SketchDataFull?,
        var created: String = DateTimeUtil.timestamp(),
        var sentences: List<SentenceFull>,
        var services: MutableList<String>,
        var clientComment: CommentFull?,
        var masterComment: CommentFull?
)

data class Order(
        var id: Int,
        var clientId: Int?,
        var masterId: Int?,
        var sketchDataId: Int?,
        var status: Int = 0,
        var price: Int = 0,
        var name: String,
        var description: String,
        var isPrivate: Boolean = false,
        var photos: MutableList<String>,
        var services: MutableList<Int>,
        var sketchData: SketchData? = null,
        var created: String = DateTimeUtil.timestamp()
)

data class OrderShort(
        var masterId: Int?,
        var sketchId: Int?,
        var cityId: Int?,
        var status: Int = 0,
        var price: Int = 0,
        var name: String,
        var description: String,
        var sketchData: SketchData?,
        var isPrivate: Boolean = false,
        var services: MutableList<Int>
)

data class OrderExtraShort(
        var orderId: Int,
        var masterId: Int?,
        var clientId: Int?,
        var status: Int = 0,
        var isPrivate: Boolean
)

data class OrderPreview (
        var id: Int,
        var name: String,
        var price: Int,
        var status: Int,
        var sentencesCount: Int
)

data class AvailableOrderPreview (
        var id: Int,
        var price: Int,
        var name: String,
        var description: String,
        var created: String = DateTimeUtil.timestamp()
)

data class UpdateOrdersStatusPayload (
        var orderId: Int,
        var clientId: Int? = null,
        var masterId: Int? = null,
        var status: Int = 3
)

data class OrderName(
        var id: Int,
        var name: String
)