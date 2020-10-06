package com.perevodchik.security

import io.micronaut.http.HttpRequest
import io.micronaut.security.token.reader.TokenReader
import java.util.*

import javax.inject.Singleton

@Singleton
internal class AccessTokenReader : TokenReader {
    override fun findToken(request: HttpRequest<*>): Optional<String> {
        val token = request.parameters.get("access_token", String::class.java)
        println("token $token")
        return token
    }
}