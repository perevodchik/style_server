package com.perevodchik.repository

import com.perevodchik.domain.Style

interface StylesService {
    fun createStyle(style: Style): Style
    fun getAllStyles(): List<Style>
}