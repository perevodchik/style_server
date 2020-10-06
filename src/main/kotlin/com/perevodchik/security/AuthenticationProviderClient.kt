package com.perevodchik.security

import com.perevodchik.domain.Phone
import com.perevodchik.repository.UsersService
import com.perevodchik.repository.MastersService
import io.micronaut.http.HttpRequest
import io.micronaut.security.authentication.*
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.FlowableEmitter
import org.reactivestreams.Publisher
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticationProviderClient : AuthenticationProvider {

    @Inject
    lateinit var usersService: UsersService

    override fun authenticate(httpRequest: HttpRequest<*>?, authenticationRequest: AuthenticationRequest<*, *>): Publisher<AuthenticationResponse?>? {
        return Flowable.create({ emitter: FlowableEmitter<AuthenticationResponse?> ->
            val user = usersService.getByPhone((authenticationRequest.identity as String))
            if (user != null) {
                if(AuthStorage.isContainsCode(Phone(authenticationRequest.identity as String), authenticationRequest.secret as String)) {
                    val params = mutableMapOf<String, Any>()
                    params["id"] = user.id
                    params["username"] = user.phone
                    params["role"] = user.role
                    emitter.onNext(UserDetails(authenticationRequest.identity as String, ArrayList(), params))
                    emitter.onComplete()
                } else
                    emitter.onError(AuthenticationException(AuthenticationFailed()))
            } else
                emitter.onError(AuthenticationException(AuthenticationFailed()))
        }, BackpressureStrategy.ERROR)
    }
}