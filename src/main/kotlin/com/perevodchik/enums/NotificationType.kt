package com.perevodchik.enums

enum class NotificationType(_type: Int) {
    PROFILE_NEW_COMMENT(0),
    ORDER_NEW_REQUEST(1),
    ORDER_CANCELLED_BY_CLIENT(2),
    ORDER_CANCELLED_BY_MASTER(3),
    ORDER_FINISHED_BY_CLIENT(4),
    ORDER_FINISHED_BY_MASTER(5),
    ORDER_SELECT_BY_MASTER(6),
    ORDER_NEW_SENTENCE(7);

    val value = _type
}