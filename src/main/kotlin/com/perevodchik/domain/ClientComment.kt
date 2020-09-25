package com.perevodchik.domain

data class ClientComment (
        var id: Int,
        var clientId: Int,
        var masterId: Int,
        var text: String,
        var rate: Double
)