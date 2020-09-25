package com.perevodchik.repository.impl

import com.perevodchik.domain.Position
import com.perevodchik.repository.PositionService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PositionServiceImpl: PositionService {

    @Inject
    lateinit var pool: io.reactiverse.reactivex.pgclient.PgPool

    override fun createPosition(position: Position): Position {
        val r = pool.rxQuery("INSERT INTO positions (name) VALUES () RETURNING positions.id;").blockingGet()
        val i = r.iterator()
        if(i.hasNext())
            position.id = i.next().getInteger("id")
        return position
    }

    override fun getAllPositions(): List<Position> {
        val positions = mutableListOf<Position>()
        val r = pool.rxQuery("SELECT * FROM positions").blockingGet()
        val i = r.iterator()
        while(i.hasNext()) {
            val row = i.next()
            val position = Position(
                    row.getInteger("id"),
                    row.getString("name")
            )
            positions.add(position)
        }
        return positions
    }

}