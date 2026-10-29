package com.github.nizienko.roboarm

import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class MessageResponse(val text: String)
private val messageResponseFlow = MutableSharedFlow<MessageResponse>()
private val sharedFlow = messageResponseFlow.asSharedFlow()

fun main() {
    embeddedServer(Netty, port = 6593) {
        install(WebSockets)

        routing {
            webSocket("/ws") {
                send("You are connected to WebSocket!")

                val job = launch {
                    sharedFlow.collect { message ->
                        send(message.text)
                    }
                }

                runCatching {
                    incoming.consumeEach { frame ->
                        if (frame is Frame.Text) {
                            val receivedText = frame.readText()
                            val messageResponse = MessageResponse(receivedText)
                            println(messageResponse)
                            messageResponseFlow.emit(messageResponse)
                        }
                    }
                }.onFailure { exception ->
                    println("WebSocket exception: ${exception.localizedMessage}")
                }.also {
                    job.cancel()
                }
            }
        }
    }.start(wait = true)
}