package com.perevodchik.controllers

import com.perevodchik.domain.User
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.PathVariable
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import java.security.Principal

@Controller("/login")
class LoginController {

    init {
        println("LoginController")
    }

    @Get("/{phone}")
    fun auth(@PathVariable phone: String): User {
        return User(0, 0, 2, 0, phone, "name", "surname", "email", "", "", isShowAddress = true, isShowPhone = true, isShowEmail = true, services = mutableListOf(), comments = mutableListOf())
    }

    @Secured(SecurityRule.IS_AUTHENTICATED)
    @Get("/current")
    fun current(principal: Principal) {
        print("$principal")
    }

}