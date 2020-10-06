package com.perevodchik.security

import io.micronaut.context.annotation.ConfigurationProperties
import io.micronaut.context.annotation.Replaces
import io.micronaut.security.token.jwt.config.JwtConfigurationProperties
import io.micronaut.security.token.jwt.generator.AccessTokenConfiguration
import io.micronaut.security.token.jwt.generator.AccessTokenConfigurationProperties
import javax.inject.Singleton

@Singleton
@Replaces(bean = AccessTokenConfigurationProperties::class)
@ConfigurationProperties(AccessTokenConfigurationProperties.PREFIX)
class CustomAccessTokenConfiguration: AccessTokenConfiguration {

    companion object {
        const val PREFIX = JwtConfigurationProperties.PREFIX + ".generator.access-token"
        const val DEFAULT_EXPIRATION = 86400
    }

    override fun getExpiration(): Int {
        return DEFAULT_EXPIRATION * 365
    }
}