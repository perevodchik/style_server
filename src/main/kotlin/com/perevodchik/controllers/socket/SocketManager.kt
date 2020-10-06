package com.perevodchik.controllers.socket

import com.perevodchik.domain.Message
import io.micronaut.websocket.WebSocketSession

class SocketManager {

    companion object {
        private var manager: SocketManager? = null
        val sockets = mutableMapOf<Int, WebSocketSession>()

        fun manager(): SocketManager {
            if(manager == null)
                manager = SocketManager()
            return manager!!
        }
    }

    fun sendMessage(message: Message, receiverId: Int) {
        sockets[receiverId]?.sendSync(message.toJson())
    }

    fun addSocket(id: Int, session: WebSocketSession) {
        sockets[id] = session
    }

    fun removeSocket(id: Int) {
        sockets.remove(id)
    }

}