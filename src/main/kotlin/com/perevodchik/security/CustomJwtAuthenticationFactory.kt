package com.perevodchik.security

import com.nimbusds.jwt.JWT
import com.nimbusds.jwt.JWTClaimsSet
import io.micronaut.context.annotation.Replaces
import io.micronaut.security.authentication.Authentication
import io.micronaut.security.token.jwt.validator.AuthenticationJWTClaimsSetAdapter
import io.micronaut.security.token.jwt.validator.DefaultJwtAuthenticationFactory
import io.micronaut.security.token.jwt.validator.JwtAuthenticationFactory
import java.util.*
import javax.inject.Singleton

@Singleton
@Replaces(bean = DefaultJwtAuthenticationFactory::class)
class CustomJwtAuthenticationFactory(): JwtAuthenticationFactory {

    override fun createAuthentication(token: JWT?): Optional<Authentication> {
        try {
            val builder = JWTClaimsSet.Builder()
//            println(token?.jwtClaimsSet?.toJSONObject().toString())
            builder.claim("username", token?.jwtClaimsSet?.getStringClaim("sub"))
            builder.claim("sub", token?.jwtClaimsSet?.getStringClaim("sub"))
            builder.claim("role", token?.jwtClaimsSet?.getIntegerClaim("role"))
            builder.claim("id", token?.jwtClaimsSet?.getIntegerClaim("id"))
            return Optional.of(AuthenticationJWTClaimsSetAdapter(builder.build()))
        } catch (e: Exception) {
            throw RuntimeException("ParseException creating authentication", e)
        }
    }
}