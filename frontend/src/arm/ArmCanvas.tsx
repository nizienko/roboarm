import { Component, Suspense, useMemo } from 'react';
import type { ReactNode } from 'react';
import { Canvas, useFrame } from '@react-three/fiber';
import { Html, OrbitControls, useGLTF } from '@react-three/drei';
import type { ArmState } from './ArmState';
import { prepareArmModel } from './ArmModel';

interface ArmCanvasProps {
    state?: ArmState;
}

function RobotModel({ state }: ArmCanvasProps) {
    const { scene } = useGLTF(`${import.meta.env.BASE_URL}models/roboarm.glb`);
    const specification = JSON.stringify(state?.parts.map(part => part.modelNode) ?? []);
    const rig = useMemo(() => prepareArmModel(scene.clone(true), JSON.parse(specification)),
        [scene, specification]);
    useFrame(() => { if (state) rig.apply(state.parts); });
    return <>
        <primitive object={rig.model} visible={!!state} />
        {rig.missing.length > 0 && <Html fullscreen style={{ pointerEvents: 'none' }}>
            <p role="status" style={{ padding: 12, color: '#ffd992', background: '#18202bcc' }}>
                Missing model nodes: {rig.missing.join(', ')}. Their motion cannot be displayed.
            </p>
        </Html>}
    </>;
}

class ModelBoundary extends Component<{ children: ReactNode }, { failed: boolean }> {
    state = { failed: false };
    static getDerivedStateFromError() { return { failed: true }; }
    render() {
        return this.state.failed
            ? <Html center><p role="alert">Failed to load roboarm.glb.</p></Html>
            : this.props.children;
    }
}

export default function ArmCanvas({ state }: ArmCanvasProps) {
    return (
        <div style={{ width: '100%', height: 600 }}>
            <Canvas camera={{ position: [0, 1.5, 3.1], near: 0.01, far: 100 }}>
                <color attach="background" args={['#18202b']} />
                <ambientLight intensity={1.1} />
                <directionalLight position={[2, 6, 2]} intensity={2.7} />
                <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, -0.001, 0]}>
                    <planeGeometry args={[10, 10]} />
                    <meshStandardMaterial color="#333333" roughness={1} />
                </mesh>
                <ModelBoundary>
                    <Suspense fallback={<Html center>Loading model…</Html>}>
                        <RobotModel state={state} />
                    </Suspense>
                </ModelBoundary>
                <OrbitControls makeDefault target={[0, 1.5, 0]} />
            </Canvas>
        </div>
    );
}
