package com.perevodchik.domain

import java.sql.Timestamp

class Record(
        var id: Int,
        var status: Int,
        var name: String,
        var description: String,
        var price: Int,
        var client: Client,
        var user: User?,
        var sketch: Sketch,
        var createDate: Timestamp,
        var categories: List<Category>,
        var photos: List<String>
)