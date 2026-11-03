import { useEffect, useRef, useState } from "react";
import ArmCanvas from "./ArmCanvas.tsx";
import { ArmControls } from "./ArmControls.tsx";
import { isArmState } from "./ArmState.ts";
import type { ArmRequest, ArmState } from "./ArmState.ts";
import "./Arm.css";
import {IKArmControls} from "./IKArmControls.tsx";

function Arm() {
    const [connectionState, setConnectionState] = useState("Connecting");
    const [armState, setArmState] = useState<ArmState>();
    const [commandError, setCommandError] = useState("");
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
                } else if (typeof state === 'object' && state !== null && 'error' in state
                    && typeof state.error === 'string') {
                    setCommandError(state.error);
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

    function sendCommand(command: ArmRequest) {
        const websocket = socketRef.current;
        if (websocket?.readyState !== WebSocket.OPEN) return;
        try {
            setCommandError("");
            websocket.send(JSON.stringify(command));
        } catch (error) {
            setConnectionState("Failed to send command");
            console.error(error);
        }
    }

    return (
        <section>
            <div className="arm-layout">
                <ArmControls state={armState} disabled={!connected} onCommand={sendCommand} />
                <section>
                    <p>{connectionState}</p>
                    <ArmCanvas state={armState} />
                    <IKArmControls state={armState} commandError={commandError} disabled={!connected} onCommand={sendCommand} />
                </section>
            </div>
        </section>
    );
}

export default Arm;
