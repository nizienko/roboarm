package com.github.nizienko.roboarm

import com.github.nizienko.roboarm.data.ArmCommand
import io.ktor.serialization.WebsocketDeserializeException
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.receiveDeserialized
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException


fun main() {

    embeddedServer(Netty, port = 6593) {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json)
        }
        val simulator = RoboArmSimulator(this)
        routing {
            webSocket("/ws") {
                val sendJob = launch {
                    simulator.armStateFlow.collect { message ->
                        sendSerialized(message)
                    }
                }
                try {
                    while (isActive) {
                        val command: ArmCommand? = try {
                            receiveDeserialized<ArmCommand>()
                        } catch (c: CancellationException) {
                            throw c
                        } catch (_: ClosedReceiveChannelException) {
                            break
                        } catch (t: WebsocketDeserializeException) {
                            println(t.message)
                            null
                        }
                        command?.let { simulator.reportCommand(it) }
                    }
                } finally {
                    sendJob.cancel()
                }
            }
        }
    }.start(wait = true)
}