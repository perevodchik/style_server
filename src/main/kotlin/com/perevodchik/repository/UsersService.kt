package com.perevodchik.repository

import com.perevodchik.domain.User
import com.perevodchik.domain.UserShortData
import com.perevodchik.domain.UserUpdatePayload
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.security.authentication.Authentication

interface UsersService {
    fun getMasters(offset: Int, limit: Int, cities: String, services: String, withHighRate: Boolean): List<UserShortData>
    fun getById(id: Int): User?
    fun getByPhone(phone: String): User?
    fun getCurrent(authentication: Authentication): User?
    fun getUserRate(id: Int): Double
    fun create(user: User): User?
    fun update(user: UserUpdatePayload, authentication: Authentication): UserUpdatePayload
    fun updatePrivateSettings(setting: Int, value: Boolean, authentication: Authentication): Boolean
    fun upload(phone: String, name: String, upload: StreamingFileUpload): String
}