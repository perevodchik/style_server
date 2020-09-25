package com.perevodchik.domain

data class Sketch(
        var id: Int,
        var ownerId: Int,
        var price: Int,
        var time: Int,
        var width: Int,
        var height: Int,
        var positionId: Int,
        var styleId: Int,
        var tags: String,
        var description: String,
        var isColored: Boolean
)

data class SketchFull(
        var id: Int,
        var ownerId: Int,
        var price: Int,
        var time: Int,
        var width: Int,
        var height: Int,
        var masterFullName: String,
        var masterAvatar: String,
        var tags: String,
        var description: String,
        var photos: String,
        var isColored: Boolean,
        var isFavorite: Boolean,
        var position: Position,
        var style: Style
)

data class SketchPreview (
        var id: Int,
        var ownerId: Int,
        var price: Int,
        var photos: String
)