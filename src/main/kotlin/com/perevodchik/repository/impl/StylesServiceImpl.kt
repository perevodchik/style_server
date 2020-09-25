package com.perevodchik.repository.impl

import com.perevodchik.domain.Style
import com.perevodchik.repository.StylesService
import javax.inject.Inject

class StylesServiceImpl: StylesService {

    @Inject
    lateinit var pool: io.reactiverse.reactivex.pgclient.PgPool

    override fun createStyle(style: Style): Style {
        val r = pool.rxQuery("INSERT INTO styles (name) VALUES () RETURNING styles.id;").blockingGet()
        val i = r.iterator()
        if(i.hasNext())
            style.id = i.next().getInteger("id")
        return style
    }

    override fun getAllStyles(): List<Style> {
        val styles = mutableListOf<Style>()
        val r = pool.rxQuery("SELECT * FROM styles").blockingGet()
        val i = r.iterator()
        while(i.hasNext()) {
            val row = i.next()
            val style = Style(
                    row.getInteger("id"),
                    row.getString("name")
            )
            styles.add(style)
        }
        return styles
    }
}