package com.perevodchik.repository

import com.perevodchik.domain.Comment
import com.perevodchik.domain.User
import com.perevodchik.domain.UserShortData
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.security.authentication.Authentication
import java.security.Principal

interface UsersService {
    fun getMasters(offset: Int, limit: Int): List<UserShortData>
    fun getById(id: Int): User?
    fun getByPhone(phone: String): User?
    fun getCurrent(authentication: Authentication): User?
    fun create(user: User): User?
    fun update(user: User, authentication: Authentication): User
    fun updatePrivateSettings(setting: Int, value: Boolean, authentication: Authentication): Boolean
    fun upload(phone: String, name: String, upload: StreamingFileUpload)
}