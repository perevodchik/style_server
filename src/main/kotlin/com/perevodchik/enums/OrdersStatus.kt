package com.perevodchik.enums

enum class OrdersStatus(_type: Int) {
    WAITING_TO_RESPONSE(0),
    FINISHED(1),
    IN_WORK(2),
    WAITING_RESPONSE_FROM_MASTER(3),
    CANCELED(4);

    val type = _type
}