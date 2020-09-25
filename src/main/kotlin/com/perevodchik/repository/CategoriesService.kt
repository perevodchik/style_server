package com.perevodchik.repository

import com.perevodchik.domain.Category
import com.perevodchik.domain.Service

interface CategoriesService {
    fun getAll(): MutableList<Category>
    fun createCategory(category: Category): Category
    fun createService(service: Service): Service
}