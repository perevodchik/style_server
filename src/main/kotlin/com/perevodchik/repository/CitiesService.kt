package com.perevodchik.repository

import com.perevodchik.domain.City

interface CitiesService {
    fun get(): List<City>
}