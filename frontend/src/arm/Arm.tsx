import { useEffect, useRef, useState } from "react";
import ArmCanvas from "./ArmCanvas.tsx";
import { ArmControls } from "./ArmControls.tsx";
import { isArmState } from "./ArmState.ts";
import type { ArmCommand, ArmState } from "./ArmState.ts";
import "./Arm.css";

function Arm() {
    const [connectionState, setConnectionState] = useState("Connecting");
    const [armState, setArmState] = useState<ArmState>();
    const socketRef = useRef<WebSocket | null>(null);
    const connected = connectionState === "Connected";

    useEffect(() => {
        const websocket = new WebSocket("ws://localhost:6593/ws");
        socketRef.current = websocket;

        const onOpen = () => setConnectionState("Connected");
        const onClose = () => setConnectionState("Connection closed");
        const onError = () => setConnectionState("Error");
        const onMessage = (event: MessageEvent<string>) => {
            try {
                const state: unknown = JSON.parse(event.data);
                if (isArmState(state)) {
                    setArmState(state);
                } else {
                    console.warn("Wrong state", state);
                }
            } catch (error) {
                console.error(error);
            }
        };

        websocket.addEventListener("open", onOpen);
        websocket.addEventListener("close", onClose);
        websocket.addEventListener("message", onMessage);
        websocket.addEventListener("error", onError);
        return () => {
            socketRef.current = null;
            websocket.removeEventListener("open", onOpen);
            websocket.removeEventListener("close", onClose);
            websocket.removeEventListener("message", onMessage);
            websocket.removeEventListener("error", onError);
            websocket.close();
        };
    }, []);

    function sendCommand(command: ArmCommand) {
        const websocket = socketRef.current;
        if (websocket?.readyState !== WebSocket.OPEN) return;
        try {
            websocket.send(JSON.stringify(command));
        } catch (error) {
            setConnectionState("Failed to send command");
            console.error(error);
        }
    }

    return (
        <section>
            <p role="status">{connectionState}</p>
            <div className="arm-layout">
                <ArmControls state={armState} disabled={!connected} onCommand={sendCommand} />
                <ArmCanvas state={armState} />
            </div>
        </section>
    );
}

export default Arm;
