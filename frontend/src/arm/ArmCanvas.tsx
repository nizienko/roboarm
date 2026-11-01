import { Canvas } from "@react-three/fiber";
import { Box, OrbitControls } from "@react-three/drei";
import { Matrix4, Quaternion, Vector3 } from "three";
import type { ArmState, Point3D } from "./ArmState";

interface ArmCanvasProps {
    state?: ArmState;
}

function toScene(p: Point3D): [number, number, number] {
    return [p.x, p.z, -p.y];
}

export default function ArmCanvas({ state }: ArmCanvasProps) {
    return (
        <div style={{ width: "100%", height: 600 }}>
            <Canvas camera={{
                position: [50, 40, 60],
                near: 0.1,
                far: 1000,
            }}>
                <color attach="background" args={["#18202b"]} />

                <gridHelper args={[100, 20]} />

                {state?.parts.map((part, index) => {
                    const start = new Vector3(...toScene(part.mesh.startPoint));
                    const end = new Vector3(...toScene(part.mesh.endPoint));
                    const direction = end.clone().sub(start);
                    const length = direction.length();
                    if (length === 0) return null;

                    const center = start.clone().add(end).multiplyScalar(0.5);
                    const axisY = direction.normalize();
                    // Two endpoints define the long axis, but leave roll undefined.
                    // Keep the cross-section aligned with scene up instead of using
                    // the shortest rotation, which twists as the mast turns.
                    const reference = Math.abs(axisY.y) > 0.999
                        ? new Vector3(0, 0, 1)
                        : new Vector3(0, 1, 0);
                    const axisX = new Vector3().crossVectors(axisY, reference).normalize();
                    const axisZ = new Vector3().crossVectors(axisX, axisY).normalize();
                    const rotation = new Quaternion().setFromRotationMatrix(
                        new Matrix4().makeBasis(axisX, axisY, axisZ),
                    );

                    return (
                        <Box
                            key={part.id}
                            args={[1, length, 1]}
                            position={center}
                            quaternion={rotation}
                        >
                            <meshBasicMaterial
                                color={["#38bdf8", "#fbbf24", "#34d399"][index % 3]}
                            />
                        </Box>
                    );
                })}

                <OrbitControls
                    makeDefault
                    target={[0, 15, 0]}
                />
            </Canvas>
        </div>
    );
}
