package com.perevodchik.repository.impl

import com.perevodchik.domain.City
import com.perevodchik.repository.CitiesService
import javax.inject.Inject

class CitiesServiceImpl: CitiesService {

    @Inject
    lateinit var pool: io.reactiverse.reactivex.pgclient.PgPool

    override fun get(): List<City> {
        val cities = mutableListOf<City>()
        val r = pool.rxQuery(
                "SELECT * FROM cities"
        ).blockingGet()
        val i = r.iterator()
        while(i.hasNext()) {
            val row = i.next()
            val city = City(
                    row.getInteger("id"),
                    row.getString("name")
            )
            cities.add(city)
        }
        return cities
    }

}