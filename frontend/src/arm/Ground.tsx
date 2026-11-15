import { useEffect, useMemo } from 'react';
import { CanvasTexture, SRGBColorSpace, Vector3 } from 'three';

const origin = new Vector3(0, 0.02, 0);
const xDirection = new Vector3(1, 0, 0);
// Backend (x, y, z) maps to scene (x, z, -y).
const yDirection = new Vector3(0, 0, -1);
const negativeXDirection = xDirection.clone().negate();
const negativeYDirection = yDirection.clone().negate();
const xColor = '#f87171';
const yColor = '#74d99f';

function Label({ text, position, color }: {
    text: string;
    position: [number, number, number];
    color: string;
}) {
    const texture = useMemo(() => {
        const canvas = document.createElement('canvas');
        canvas.width = 256;
        canvas.height = 128;
        const context = canvas.getContext('2d')!;
        context.font = 'bold 64px system-ui, sans-serif';
        context.textAlign = 'center';
        context.textBaseline = 'middle';
        context.fillStyle = color;
        context.fillText(text, 128, 64);
        const result = new CanvasTexture(canvas);
        result.colorSpace = SRGBColorSpace;
        return result;
    }, [text, color]);
    useEffect(() => () => texture.dispose(), [texture]);

    return <sprite position={position} scale={[0.7, 0.35, 1]}>
        <spriteMaterial map={texture} transparent depthWrite={false} toneMapped={false} />
    </sprite>;
}

export function Ground() {
    return <>
        <mesh rotation={[-Math.PI / 2, 0, 0]} position={[0, -0.01, 0]}>
            <planeGeometry args={[10, 10]} />
            <meshStandardMaterial color="#333333" roughness={1} />
        </mesh>
        <gridHelper args={[10, 10, '#737b85', '#515962']} />
        <arrowHelper args={[xDirection, origin, 2.6, xColor, 0.18, 0.12]} />
        <arrowHelper args={[yDirection, origin, 2.6, yColor, 0.18, 0.12]} />
        <arrowHelper args={[negativeXDirection, origin, 2.6, xColor, 0.18, 0.12]} />
        <arrowHelper args={[negativeYDirection, origin, 2.6, yColor, 0.18, 0.12]} />
        <Label text="+X" position={[2.8, 0.22, 0]} color={xColor} />
        <Label text="+Y" position={[0, 0.22, -2.8]} color={yColor} />
        <Label text="−X" position={[-2.8, 0.22, 0]} color={xColor} />
        <Label text="−Y" position={[0, 0.22, 2.8]} color={yColor} />
    </>;
}
