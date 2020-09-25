package com.perevodchik.repository.impl

import com.perevodchik.domain.Category
import com.perevodchik.domain.Service
import com.perevodchik.repository.CategoriesService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoriesServiceImpl: CategoriesService {

    @Inject
    lateinit var pool: io.reactiverse.reactivex.pgclient.PgPool

    override fun getAll(): MutableList<Category> {
        val data = mutableListOf<Category>()
        val r0 = pool.rxQuery("SELECT * FROM categories").blockingGet()
        val i0 = r0.iterator()
        while(i0.hasNext()) {
            val row = i0.next()
            val category = Category(
                    row.getInteger("id"),
                    row.getString("name"),
                    mutableListOf()
            )
            data.add(category)
        }
        val r1 = pool.rxQuery("SELECT * FROM services").blockingGet()
        val i1 = r1.iterator()
        while(i1.hasNext()) {
            val row = i1.next()
            val service = Service(
                    row.getInteger("id"),
                    row.getInteger("category_id"),
                    row.getString("name"),
                    row.getBoolean("is_tatoo")
            )
            data.find { c -> c.id == service.categoryId }?.services?.add(service)
        }
        return data
    }

    override fun createCategory(category: Category): Category {
        val r = pool.rxQuery("INSERT INTO categories (name) VALUES ('${category.name}') RETURNING categories.id").blockingGet()
        val i = r.iterator()
        if(i.hasNext()) {
            val row = i.next()
            category.id = row.getInteger("id")
        }
        return category
    }

    override fun createService(service: Service): Service {
        val r = pool.rxQuery("INSERT INTO services (category_id, name, is_tatoo) VALUES (${service.categoryId}, '${service.name}', ${service.isTatoo}) RETURNING services.id").blockingGet()
        val i = r.iterator()
        if(i.hasNext()) {
            val row = i.next()
            service.id = row.getInteger("id")
        }
        return service
    }
}