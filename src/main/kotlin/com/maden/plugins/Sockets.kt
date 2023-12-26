package com.maden.plugins

import com.maden.model.Room
import com.maden.model.SocketSessions
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import java.time.Duration

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(25)
        timeout = Duration.ofSeconds(25)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        val rooms = mutableMapOf<String, Room>()

        webSocket("/screenMirroring") {
            val roomId = call.parameters["roomId"] ?: throw IllegalArgumentException("Room ID not specified")
            var isReceiver = false
            try {
                call.parameters["isReceiver"]?.let {
                    isReceiver = it.toBoolean()
                }
            } catch (_: Exception) {
            }

            val client = this

            // Create or get the existing room
            val room = rooms.getOrPut(roomId) { Room(roomId) }


            // Add the client to the room
            room.clients[client] = SocketSessions(isReceiver)


            if (room.clients.size > 1)
                room.clients.forEach { it.key.send(Frame.Text("Start")) }

            // Handle incoming messages
            try {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Binary -> {
                            room.clients.forEach {
                                if (!it.value.isReceived) {
                                    it.key.send(Frame.Binary(true, frame.readBytes()))
                                }
                            }
                        }

                        else -> {}
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                // Channel closed
            } finally {
                // Remove the client from the room when the connection is closed
                with(room.clients) {
                    remove(client)

                    if (size == 1)
                        forEach { it.key.send(Frame.Text("Stop")) }

                    // If there are no more clients in the room, remove the room from the map
                    if (isEmpty()) rooms.remove(roomId)
                }
            }
        }
    }
}
