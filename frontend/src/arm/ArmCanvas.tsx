import type {ArmState} from "./ArmState.ts";

interface ArmCanvasProps {
    state?: ArmState
}

function ArmCanvas({state}: ArmCanvasProps) {
    return (
        <section>
            Data: {state?.time}
        </section>
    )
}

export default ArmCanvas