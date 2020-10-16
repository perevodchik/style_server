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
        println("receiver $receiverId, sockets => $sockets")
        val socket = sockets[receiverId]
        print("send $message to $socket")
        socket?.sendSync(message.toJson())
    }

    fun addSocket(id: Int, session: WebSocketSession) {
        println("add socket for user $id [$session]")
        sockets[id] = session
    }

    fun removeSocket(id: Int) {
        val r = sockets.remove(id)
        println("remove socket from user $id [$r]")
    }

}