import { Component, Suspense, useLayoutEffect, useMemo } from 'react';
import type { ReactNode } from 'react';
import { Canvas } from '@react-three/fiber';
import { OrbitControls, useGLTF } from '@react-three/drei';
import type { ArmState } from './ArmState';
import { prepareArmModel } from './ArmModel';

interface ArmCanvasProps {
    state?: ArmState;
}

function RobotScene({ state }: ArmCanvasProps) {
    const { scene } = useGLTF(`${import.meta.env.BASE_URL}models/roboarm.glb`);
    const rig = useMemo(() => prepareArmModel(scene.clone(true)), [scene]);
    const missing = state ? rig.missingNodes(state.parts) : [];
    useLayoutEffect(() => {
        rig.apply(state?.parts ?? []);
    }, [rig, state]);
    useLayoutEffect(() => () => rig.dispose(), [rig]);

    return <>
        <Canvas camera={{ position: [3, 2.4, 5], fov: 45, near: 0.01, far: 100 }}>
            <color attach="background" args={['#18202b']} />
            <ambientLight intensity={1.1} />
            <directionalLight position={[2, 6, 2]} intensity={2.7} />
            <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, -0.001, 0]}>
                <planeGeometry args={[10, 10]} />
                <meshStandardMaterial color="#333333" roughness={1} />
            </mesh>
            <primitive object={rig.model} visible={!!state} />
            <OrbitControls makeDefault target={[0.5, 1.5, 0]} />
        </Canvas>
        {missing.length > 0 && <p className="arm-scene-message" role="status">
            Missing model nodes: {missing.join(', ')}. Their motion cannot be displayed.
        </p>}
    </>;
}

class ModelBoundary extends Component<{ children: ReactNode }, { failed: boolean }> {
    state = { failed: false };
    static getDerivedStateFromError() { return { failed: true }; }
    render() {
        return this.state.failed
            ? <p role="alert">Failed to load the arm model.</p>
            : this.props.children;
    }
}

export default function ArmCanvas({ state }: ArmCanvasProps) {
    return (
        <div className="arm-scene">
            <ModelBoundary>
                <Suspense fallback={<p role="status">Loading model…</p>}>
                    <RobotScene state={state} />
                </Suspense>
            </ModelBoundary>
        </div>
    );
}
