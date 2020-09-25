package com.perevodchik.repository

import com.perevodchik.domain.Position

interface PositionService {
    fun createPosition(position: Position): Position
    fun getAllPositions(): List<Position>
}