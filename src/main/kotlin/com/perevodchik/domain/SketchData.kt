package com.perevodchik.domain

data class SketchData(
        var id: Int,
        var positionId: Int,
        var styleId: Int,
        var width: Int,
        var height: Int,
        var isColored: Boolean
)

data class SketchDataFull(
        var id: Int,
        var position: Position,
        var style: Style,
        var width: Int,
        var height: Int,
        var isColored: Boolean
)