package com.perevodchik.repository

import com.perevodchik.domain.*
import io.micronaut.http.HttpResponse
import io.micronaut.http.multipart.StreamingFileUpload
import java.security.Principal

interface MastersService {
    fun getServiceByMaster(id: Int): List<ServiceWrapper>
    fun getFullServiceByMaster(categories: List<Category>, services: List<ServiceWrapper>): List<Category>
    fun addService(serviceWrapper: ServiceWrapper): ServiceWrapper
    fun checkService(userId: Int, serviceId: Int): Boolean
    fun updateService(serviceWrapper: ServiceWrapper): ServiceWrapper
    fun deleteService(serviceWrapper: ServiceWrapper): Int
    fun addMasterPortfolio(userId: Int, fileName: String, upload: StreamingFileUpload): PortfolioItem?
    fun getMasterPortfolio(userId: Int): List<PortfolioItem>
    fun getMasterPhotos(userId: Int): String
    fun deleteMasterPortfolio(masterId: Int, portfolioItemId: Int): Boolean
}