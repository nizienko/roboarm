package com.github.nizienko.roboarm

import com.github.nizienko.roboarm.data.ArmCommand
import com.github.nizienko.roboarm.data.ArmRequest
import com.github.nizienko.roboarm.data.ArmTargetCommand
import com.github.nizienko.roboarm.data.ArmCommandError
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.microseconds


fun main() {

    embeddedServer(Netty, port = 6593) {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json {
                encodeDefaults = true
            })
        }
        val simulator = RoboArmSimulator(this)
        routing {
            webSocket("/ws") {
                val sendJob = launch {
                    simulator.armStateFlow.collect { message ->
                        sendSerialized(message)
                        delay(30.microseconds)
                    }
                }
                try {
                    while (isActive) {
                        val command: ArmRequest? = try {
                            receiveDeserialized<ArmRequest>()
                        } catch (c: CancellationException) {
                            throw c
                        } catch (_: ClosedReceiveChannelException) {
                            break
                        } catch (t: WebsocketDeserializeException) {
                            println(t.message)
                            null
                        }
                        when (command) {
                            is ArmCommand -> simulator.reportCommand(command)
                            is ArmTargetCommand -> if (!simulator.reportTarget(command)) {
                                sendSerialized(ArmCommandError("Target is unreachable or outside joint limits"))
                            }
                            null -> Unit
                        }
                    }
                } finally {
                    sendJob.cancel()
                }
            }
        }
    }.start(wait = true)
}
