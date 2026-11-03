interface ArmSocketHandlers {
    onSocket: (socket: WebSocket | null) => void;
    onOpen: () => void;
    onClose: () => void;
    onMessage: (event: MessageEvent<string>) => void;
}

export function connectArmSocket(url: string, handlers: ArmSocketHandlers) {
    let stopped = false;
    let retryDelay = 500;
    let timer: ReturnType<typeof setTimeout> | undefined;
    let disconnect = () => {};

    function connect() {
        const socket = new WebSocket(url);
        handlers.onSocket(socket);
        const onOpen = () => {
            retryDelay = 500;
            handlers.onOpen();
        };
        const onClose = () => {
            detach();
            handlers.onSocket(null);
            if (stopped) return;
            handlers.onClose();
            timer = setTimeout(connect, retryDelay);
            retryDelay = Math.min(retryDelay * 2, 5000);
        };
        const onError = () => socket.close();
        function detach() {
            socket.removeEventListener("open", onOpen);
            socket.removeEventListener("close", onClose);
            socket.removeEventListener("error", onError);
            socket.removeEventListener("message", handlers.onMessage);
        }
        socket.addEventListener("open", onOpen);
        socket.addEventListener("close", onClose);
        socket.addEventListener("error", onError);
        socket.addEventListener("message", handlers.onMessage);
        disconnect = () => {
            detach();
            socket.close();
        };
    }

    connect();
    return () => {
        stopped = true;
        clearTimeout(timer);
        disconnect();
        handlers.onSocket(null);
    };
}
