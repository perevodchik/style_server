package com.perevodchik.repository.impl

import com.perevodchik.domain.User
import com.perevodchik.domain.UserShortData
import com.perevodchik.repository.UsersService
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.security.authentication.Authentication
import io.reactivex.Single
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsersServiceImpl: UsersService {

    @Inject
    lateinit var pool: io.reactiverse.reactivex.pgclient.PgPool

    override fun getMasters(offset: Int, limit: Int): List<UserShortData> {
        val masters = mutableListOf<UserShortData>()
        val r = pool.rxQuery("SELECT users.id, users.name, users.surname, users.avatar, users.city_id, string_agg(user_portfolios.image, ',') as portfolio, AVG(COALESCE(comments.rate, 0)) as rate FROM users LEFT JOIN user_portfolios ON users.id = user_portfolios.user_id LEFT JOIN comments ON users.id = comments.target_id WHERE ROLE = 1 GROUP BY users.id ORDER BY rate DESC OFFSET $offset LIMIT $limit;").blockingGet()
        val i = r.iterator()
        while(i.hasNext()) {
            val row = i.next()
            val masterShortData = UserShortData(
                    id = row.getInteger("id"),
                    cityId = row.getInteger("city_id"),
                    rate = row.getDouble("rate") ?: 0.0,
                    name = row.getString("name"),
                    surname = row.getString("surname"),
                    avatar = row.getString("avatar"),
                    portfolio = row.getString("portfolio") ?: ""
            )
            masters.add(masterShortData)
        }
        return masters
    }

    override fun getById(id: Int): User? {
        val result = pool.rxQuery("SELECT * FROM Users WHERE id = $id;").blockingGet()
        val i = result.iterator()
        if(i.hasNext()) {
            val row = i.next()
            val user = User(
                    id = row.getInteger("id"),
                    cityId = row.getInteger("city_id"),
                    role = row.getInteger("role"),
                    phone = row.getString("phone"),
                    name = row.getString("name"),
                    surname = row.getString("surname"),
                    email = row.getString("email"),
                    address = row.getString("address") ?: "",
                    avatar = row.getString("avatar"),
                    isShowAddress = row.getBoolean("is_show_address"),
                    isShowPhone = row.getBoolean("is_show_phone"),
                    isShowEmail = row.getBoolean("is_show_email")
            )
            return user
        }
        return null
    }

    override fun getByPhone(phone: String): User? {
        val result = pool.rxQuery("SELECT * FROM users WHERE phone = '$phone';").blockingGet()
        val i = result.iterator()
        if(i.hasNext()) {
            val row = i.next()
            val user = User(
                    id = row.getInteger("id"),
                    cityId = row.getInteger("city_id"),
                    role = row.getInteger("role"),
                    phone = row.getString("phone"),
                    name = row.getString("name"),
                    surname = row.getString("surname"),
                    email = row.getString("email"),
                    address = row.getString("address") ?: "",
                    avatar = row.getString("avatar"),
                    isShowAddress = row.getBoolean("is_show_address"),
                    isShowPhone = row.getBoolean("is_show_phone"),
                    isShowEmail = row.getBoolean("is_show_email")
            )
            if(!user.isShowAddress)
                user.address = ""
            if(!user.isShowPhone)
                user.phone = ""
            if(!user.isShowEmail)
                user.email = ""
            return user
        }
        return null
    }

    override fun getCurrent(authentication: Authentication): User? {
        val phone = authentication.attributes["username"] as String
        return getByPhone(phone)
    }

    override fun create(user: User): User? {
        val usersByPhone = pool.rxQuery("SELECT * FROM users WHERE phone = '${user.phone}'").blockingGet()
        println("exist ? ${usersByPhone.rowCount()}")
        if(usersByPhone.rowCount() == 0) {
            val r = pool.rxQuery(
                    "INSERT INTO users (" +
                            "city_id, name, surname, avatar, phone, address, email, role" +
                            ") VALUES (" +
                            "${user.cityId}, '${user.name}', '${user.surname}', '${user.avatar}', '${user.phone}', '', '${user.email}', ${user.role}" +
                            ") RETURNING users.id;"
            ).blockingGet()
            val i = r.iterator()
            if (i.hasNext()) {
                val row = i.next()
                user.id = row.getInteger("id")
                return user
            }
        }
        return null
    }

    override fun update(user: User, authentication: Authentication): User {
        val value = getByPhone(authentication.attributes["username"] as String)
        if(value != null) {
            val r = pool.rxQuery(
                    "UPDATE users SET name = '${user.name}', surname = '${user.surname}', email = '${user.email}', address = '${user.address}', city_id = ${user.cityId} WHERE id = ${value.id};"
            ).blockingGet()
        }
        return user
    }

    override fun updatePrivateSettings(setting: Int, value: Boolean, authentication: Authentication): Boolean {
        if(setting > 2 || setting < 0)
            return false
        val settingName = when(setting) {
            0 -> "is_show_address"
            1 -> "is_show_phone"
            2 -> "is_show_email"
            else -> "is_show_address"
        }
        val r = pool.rxQuery("UPDATE users SET $settingName = $value WHERE phone = '${authentication.attributes["username"] as String}';").blockingGet()
        return r.rowCount() > 0
    }

    override fun upload(phone: String, name: String, upload: StreamingFileUpload) {
        val tempFile = File.createTempFile(upload.filename, name)
        val uploadPublisher = upload.transferTo(tempFile)
        Single.fromPublisher(uploadPublisher)
    }
}