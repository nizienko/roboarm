import type { Point3D } from './ArmState';

const crosshair = new Float32Array([
    -0.12, 0, 0, 0.12, 0, 0,
    0, -0.12, 0, 0, 0.12, 0,
    0, 0, -0.12, 0, 0, 0.12,
]);

export function IKTargetCursor({ target }: { target: Point3D }) {
    // IK uses Z-up coordinates; the scene is Y-up.
    return <group position={[target.x, 0, -target.y]}>
        <group position={[0, target.z, 0]}>
            <lineSegments renderOrder={10}>
                <bufferGeometry>
                    <bufferAttribute attach="attributes-position" args={[crosshair, 3]} />
                </bufferGeometry>
                <lineBasicMaterial color="#67e8f9" depthTest={false} depthWrite={false} toneMapped={false} />
            </lineSegments>
            <mesh renderOrder={10}>
                <sphereGeometry args={[0.02, 12, 8]} />
                <meshBasicMaterial color="#67e8f9" depthTest={false} depthWrite={false} toneMapped={false} />
            </mesh>
        </group>
        <mesh position={[0, 0.02, 0]} renderOrder={10}>
            <sphereGeometry args={[0.02, 12, 8]} />
            <meshBasicMaterial color="#67e8f9" depthTest={false} depthWrite={false} toneMapped={false} />
        </mesh>
    </group>;
}
