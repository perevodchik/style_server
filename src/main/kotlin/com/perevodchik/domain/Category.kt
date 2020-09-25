package com.perevodchik.domain

data class Category (
        var id: Int,
        var name: String,
        var services: MutableList<Service> = mutableListOf()
)