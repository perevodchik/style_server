package com.perevodchik.controllers.socket

import com.perevodchik.repository.UsersService
import io.micronaut.security.annotation.Secured
import io.micronaut.security.rules.SecurityRule
import io.micronaut.websocket.CloseReason
import io.micronaut.websocket.WebSocketSession
import io.micronaut.websocket.annotation.*
import javax.inject.Inject

@ServerWebSocket("/v1/ws/socket")
@Secured(SecurityRule.IS_AUTHENTICATED)
class ChatSocketController {
    @Inject
    lateinit var usersService: UsersService

    @OnOpen
    fun onOpenSocket(session: WebSocketSession) {
        val user = usersService.getByPhone(session.userPrincipal.get().name)
        SocketManager.manager().addSocket(user!!.id, session)
        println("onOpenSocket [${session.userPrincipal.get().name}]")
    }

    @OnClose
    fun onClose(closeReason: CloseReason?, session: WebSocketSession) {
        val user = usersService.getByPhone(session.userPrincipal.get().name)
        SocketManager.manager().removeSocket(user!!.id)
        println("closed websocket: [${session.id}] [${closeReason?.code}] [${closeReason?.reason}]")
    }

    @OnMessage
    fun onMessage(message: String?, session: WebSocketSession) {
        println("received message from ${session.id}: $message")
    }

    @OnError
    fun onError(error: Throwable?) {
        println("an error occured: ${error?.localizedMessage}")
    }
}