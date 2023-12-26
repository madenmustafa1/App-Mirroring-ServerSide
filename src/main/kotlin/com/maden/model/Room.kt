package com.maden.model

import io.ktor.websocket.*

data class Room(
    val roomId: String,
    val clients: MutableMap<DefaultWebSocketSession, SocketSessions> = mutableMapOf()
)

data class SocketSessions(
    val isReceived: Boolean
)
