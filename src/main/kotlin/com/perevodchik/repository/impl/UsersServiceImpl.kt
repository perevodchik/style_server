package com.perevodchik.repository.impl

import com.perevodchik.domain.User
import com.perevodchik.domain.UserRegistered
import com.perevodchik.domain.UserShortData
import com.perevodchik.domain.UserUpdatePayload
import com.perevodchik.repository.UsersService
import com.perevodchik.utils.FileUtils
import io.micronaut.http.multipart.StreamingFileUpload
import io.micronaut.security.authentication.Authentication
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsersServiceImpl: UsersService {

    @Inject
    lateinit var pool: io.reactiverse.reactivex.pgclient.PgPool

    override fun getMasters(offset: Int, limit: Int, cities: String, services: String, withHighRate: Boolean): List<UserShortData> {
        val masters = mutableListOf<UserShortData>()
        val q = "SELECT u.id, u.name, u.surname, u.avatar, u.city_id, " +
                "string_agg(user_portfolios.image, ',') as portfolio, " +
                "AVG(COALESCE(comments.rate, 0)) as rate FROM users u " +
                "LEFT JOIN user_portfolios ON u.id = user_portfolios.user_id " +
                "LEFT JOIN comments ON u.id = comments.target_id " +
                "WHERE ROLE = 1 " +
                (if(cities.isNotEmpty()) "AND u.city_id IN ($cities) " else "") +
                (if(withHighRate) "AND rate >= 3.5 " else "") +
                (if(services.isNotEmpty()) {
                    var servicesString = ""
                    for(s in services.split(","))
                        servicesString += "'$s',"
                    if(servicesString.endsWith(","))
                        servicesString = servicesString.substring(0, servicesString.length - 1)
                    "AND (SELECT ARRAY[array_agg(distinct user_services.service_id)] as services FROM users LEFT JOIN user_services ON u.id = user_services.user_id) && ARRAY[$servicesString]::int[] "
                } else "") +
                "GROUP BY u.id " +
                "ORDER BY rate DESC " +
                "OFFSET $offset LIMIT $limit;"

        println(q)
        val r = pool.rxQuery(q).blockingGet()
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
            return User(
                    id = row.getInteger("id"),
                    cityId = row.getInteger("city_id"),
                    role = row.getInteger("role"),
                    rate = getUserRate(id),
                    phone = row.getString("phone"),
                    name = row.getString("name"),
                    surname = row.getString("surname"),
                    email = row.getString("email"),
                    address = row.getString("address") ?: "",
                    about = row.getString("about") ?: "",
                    avatar = row.getString("avatar"),
                    isShowAddress = row.getBoolean("is_show_address"),
                    isShowPhone = row.getBoolean("is_show_phone"),
                    isShowEmail = row.getBoolean("is_show_email")
            )
        }
        return null
    }

    override fun getByPhone(phone: String): User? {
        val result = pool.rxQuery("SELECT * FROM users WHERE phone = '$phone';").blockingGet()
        val i = result.iterator()
        if(i.hasNext()) {
            val row = i.next()
            return User(
                    id = row.getInteger("id"),
                    cityId = row.getInteger("city_id"),
                    role = row.getInteger("role"),
                    rate = getUserRate(row.getInteger("id")),
                    phone = row.getString("phone"),
                    name = row.getString("name"),
                    surname = row.getString("surname"),
                    email = row.getString("email"),
                    about = row.getString("about") ?: "",
                    address = row.getString("address") ?: "",
                    avatar = row.getString("avatar"),
                    isShowAddress = row.getBoolean("is_show_address"),
                    isShowPhone = row.getBoolean("is_show_phone"),
                    isShowEmail = row.getBoolean("is_show_email")
            )
        }
        return null
    }

    override fun getCurrent(authentication: Authentication): User? {
        val phone = authentication.attributes["username"] as String
        return getByPhone(phone)
    }

    override fun getUserRate(id: Int): Double {
        val r = pool.rxQuery("SELECT AVG(rate) as rate FROM comments WHERE target_id = $id;").blockingGet()
        return r.iterator().next().getDouble("rate") ?: 0.0
    }

    override fun create(user: UserRegistered): UserRegistered? {
        val usersByPhone = pool.rxQuery("SELECT * FROM users WHERE phone = '${user.phone}'").blockingGet()
        println("exist ? ${usersByPhone.rowCount()}")
        if(usersByPhone.rowCount() == 0) {
            val r = pool.rxQuery(
                    "INSERT INTO users (" +
                            "city_id, name, surname, avatar, phone, address, email, about, role" +
                            ") VALUES (" +
                            "${user.cityId}, '${user.name}', '${user.surname}', '', '${user.phone}', '', '', '', ${user.role}" +
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

    override fun update(user: UserUpdatePayload, authentication: Authentication): UserUpdatePayload {
        val userId = authentication.attributes["id"] as Int
        pool.rxQuery(
                "UPDATE users SET name = '${user.name}', surname = '${user.surname}', email = '${user.email}', address = '${user.address}', about = '${user.about}', city_id = ${user.cityId} WHERE id = $userId;"
        ).blockingGet()
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

    override fun upload(phone: String, name: String, upload: StreamingFileUpload): String {
        val uploadResult = FileUtils().uploadFile(upload, name)
        if(uploadResult.isNotEmpty()) {
            val r = pool.rxQuery("UPDATE users SET avatar = '$uploadResult' WHERE phone = '$phone';").blockingGet()
            if(r.rowCount() > 0)
                return uploadResult
        }
        return ""
    }
}