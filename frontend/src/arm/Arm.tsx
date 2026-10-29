import {useEffect, useState} from "react";
import ArmCanvas from "./ArmCanvas.tsx";
import type {ArmState} from "./ArmState.ts";


function Arm() {
    const [connectionState, setConnectionState] = useState<string>("no connection")
    const [armState, setArmState] = useState<ArmState>()

    useEffect(() => {
        const websocket = new WebSocket("ws://localhost:6593/ws")

        const onOpen = (event: Event) => {
            console.log("open event" + event)
            setConnectionState("Connected")
        }
        const onClose = (event: CloseEvent) => {
            console.log("open event" + event)
            setConnectionState("Disconnected")
        }
        const onMessage = (event: MessageEvent<string>) => {
            try {
                const state: unknown = JSON.parse(event.data)
                if (typeof state === "object"
                    && state !== null
                    && !Array.isArray(state)
                    && "time" in state
                    && typeof state.time === "number"
                    && Number.isSafeInteger(state.time)
                ) {
                    setArmState(state as ArmState)
                } else {
                    console.warn("Wrong state: " + state)
                }
            } catch (e) {
                console.error(e)
            }
        }

        const onError =  (event: Event) => {
            setConnectionState("Error: ," + event)
        }
        websocket.addEventListener("open", onOpen)
        websocket.addEventListener("close", onClose)
        websocket.addEventListener("message", onMessage)
        websocket.addEventListener("error", onError)
        return () => {
            websocket.removeEventListener("open", onOpen)
            websocket.removeEventListener("close", onClose)
            websocket.removeEventListener("message", onMessage)
            websocket.removeEventListener("error", onError)
            websocket.close()
        }
    }, [])
    if (connectionState === "Connected") {
        return (
            <section>
                <ArmCanvas state = {armState}/>
            </section>
        )
    }

    return (
        <section>
            <h2> {connectionState} </h2>
        </section>
    )
}

export default Arm;