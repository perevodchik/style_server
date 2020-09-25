package com.perevodchik.utils

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class DateTimeUtil {

    companion object {
        fun timestamp(): String {
            return DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
                    .withZone(ZoneOffset.UTC)
                    .format(Instant.now())
        }
    }

}