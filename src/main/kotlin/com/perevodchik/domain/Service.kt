package com.perevodchik.domain

data class Service (
        var id: Int,
        val categoryId: Int,
        val name: String,
        val isTatoo: Boolean,
        var masterService: ServiceWrapper? = null
)