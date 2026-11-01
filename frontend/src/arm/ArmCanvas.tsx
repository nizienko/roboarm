import { Canvas } from "@react-three/fiber";
import { Line, OrbitControls } from "@react-three/drei";
import type { ArmState, Point3D } from "./ArmState";

interface ArmCanvasProps {
    state?: ArmState;
}

function toScene(p: Point3D): [number, number, number] {
    return [p.x, p.z, -p.y];
}

export default function ArmCanvas({ state }: ArmCanvasProps) {
    return (
        <div style={{ width: "100%", height: 800 }}>
            <Canvas camera={{
                position: [50, 40, 60],
                near: 0.1,
                far: 1000,
            }}>
                <color attach="background" args={["#18202b"]} />

                <gridHelper args={[100, 20]} />

                {state?.parts.map((part, index) => (
                    <Line
                        key={index}
                        points={[
                            toScene(part.mesh.startPoint),
                            toScene(part.mesh.endPoint),
                        ]}
                        color={["#38bdf8", "#fbbf24", "#34d399"][index % 3]}
                        lineWidth={6}
                        depthTest={false}
                        depthWrite={false}
                        renderOrder={10 + index}
                    />
                ))}

                <OrbitControls
                    makeDefault
                    target={[0, 15, 0]}
                />
            </Canvas>
        </div>
    );
}
