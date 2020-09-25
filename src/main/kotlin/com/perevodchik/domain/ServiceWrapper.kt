package com.perevodchik.domain

data class ServiceWrapper (
        var id: Int,
        var userId: Int,
        var serviceId: Int,
        var price: Int,
        var time: Int,
        var description: String
)