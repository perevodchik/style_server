package com.perevodchik.repository.impl

import com.perevodchik.domain.*
import com.perevodchik.repository.MastersService
import com.perevodchik.utils.FileUtils
import io.micronaut.http.multipart.StreamingFileUpload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MastersServiceImpl: MastersService {

    @Inject
    lateinit var pool: io.reactiverse.reactivex.pgclient.PgPool

    override fun getServiceByMaster(id: Int): List<ServiceWrapper> {
        val list = mutableListOf<ServiceWrapper>()
        val r = pool.rxQuery("SELECT * FROM user_services WHERE user_id = $id").blockingGet()
        val i = r.iterator()
        while(i.hasNext()) {
            val row = i.next()
            val serviceWrapper = ServiceWrapper(
                    row.getInteger("id"),
                    row.getInteger("user_id"),
                    row.getInteger("service_id"),
                    row.getInteger("price"),
                    row.getInteger("time"),
                    row.getString("message")
            )
            list.add(serviceWrapper)
        }
        return list
    }

    override fun getFullServiceByMaster(categories: List<Category>, services: List<ServiceWrapper>): List<Category> {
        val i = services.iterator()
        while(i.hasNext()) {
            val wrapper = i.next()
            for(c in categories)
                for(s in c.services)
                    if(s.id == wrapper.serviceId)
                        s.masterService = wrapper
        }

        return categories
    }

    override fun addService(serviceWrapper: ServiceWrapper): ServiceWrapper {
        val r = pool.rxQuery(
                "INSERT INTO user_services (" +
                        "user_id, service_id, message, time, price" +
                        ") VALUES (" +
                        "${serviceWrapper.userId}, ${serviceWrapper.serviceId}, '${serviceWrapper.description}', ${serviceWrapper.time}, ${serviceWrapper.price}" +
                        ") RETURNING user_services.id;"
        ).blockingGet()
        val i = r.iterator()
        if(i.hasNext()) {
            val row = i.next()
            serviceWrapper.id = row.getInteger("id")
        }
        return serviceWrapper
    }

    override fun checkService(userId: Int, serviceId: Int): Boolean {
        val r = pool.rxQuery("SELECT COUNT(id) FROM user_services WHERE user_id = $userId AND id = $serviceId;").blockingGet()
        return r.rowCount() > 0
    }

    override fun updateService(serviceWrapper: ServiceWrapper): ServiceWrapper {
        val r = pool.rxQuery(
                "UPDATE user_services SET price = ${serviceWrapper.price}, time = ${serviceWrapper.time}, message = '${serviceWrapper.description}' WHERE id = ${serviceWrapper.id};"
        ).blockingGet()
        println("update ${r.rowCount()} rows")
        return serviceWrapper
    }

    override fun deleteService(serviceWrapper: ServiceWrapper): Int {
        val r = pool.rxQuery("DELETE FROM user_services WHERE id = ${serviceWrapper.id}").blockingGet()
        return r.rowCount()
    }

    override fun addMasterPortfolio(userId: Int, fileName: String, upload: StreamingFileUpload): PortfolioItem? {
        val isUpload = FileUtils().uploadFile(upload, "static/portfolios/$fileName")
        if(isUpload) {
            val r = pool.rxQuery("INSERT INTO user_portfolios (user_id, image) VALUES ($userId, '$fileName') RETURNING user_portfolios.id;").blockingGet()
            val id = r.iterator().next().getInteger("id")
            return PortfolioItem(id, userId, fileName)
        }
        return null
    }

    override fun getMasterPortfolio(userId: Int): List<PortfolioItem> {
        val portfolioItems = mutableListOf<PortfolioItem>()
        val r = pool.rxQuery("SELECT * FROM user_portfolios").blockingGet()
        val i = r.iterator()
        while(i.hasNext()) {
            val row = i.next()
            val portfolioItem = PortfolioItem(
                    row.getInteger("id"),
                    row.getInteger("user_id"),
                    row.getString("image")
            )
            portfolioItems.add(portfolioItem)
        }
        return portfolioItems
    }

    override fun getMasterPhotos(userId: Int): String {
        val r = pool.rxQuery("SELECT image FROM user_portfolios WHERE user_id = $userId;").blockingGet()
        val i = r.iterator()
        val photos = mutableListOf<String>()
        while(i.hasNext()) {
            val row = i.next()
            println(row.getString("image"))
            photos.add(row.getString("image"))
        }
        return photos.joinToString("," )
    }

    override fun deleteMasterPortfolio(userId: Int, portfolioItemId: Int): Boolean {
        val r = pool.rxQuery("SELECT * FROM user_portfolios WHERE id = $portfolioItemId").blockingGet()
        val i = r.iterator()
        if(i.hasNext()) {
            val row = i.next()
            if(row.getInteger("user_id") != userId)
            return false
            val r0 = pool.rxQuery("DELETE FROM user_portfolios WHERE id = $portfolioItemId").blockingGet()
            return if(r0.rowCount() > 0) {
                try {
                    val isDelete = FileUtils().deleteFile(row.getString("image"))
                } catch(ex: Exception) {}
                true
            } else false
        }
        return false
    }
}