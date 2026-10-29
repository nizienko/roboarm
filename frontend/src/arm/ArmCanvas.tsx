interface ArmCanvasProps {
    message?: string
}

function ArmCanvas({message}: ArmCanvasProps) {
    return (
        <section>
            Data: {message}
        </section>
    )
}

export default ArmCanvas