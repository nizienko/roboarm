import {useEffect, useState} from "react";
import ArmCanvas from "./ArmCanvas.tsx";


function Arm() {
    const [connectionState, setConnectionState] = useState<string>("no connection")
    const [message, setMessage] = useState<string>("")

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
            console.log("message ", event)
            setMessage(event.data)
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
                <ArmCanvas message = {message}/>
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