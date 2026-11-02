import { Object3D } from 'three';
import type { ArmPart } from './ArmState';

/** Each controlled node is rendered independently using its server-provided world pose. */
export function prepareArmModel(model: Object3D, nodeNames: string[]) {
    const nodes = new Map(nodeNames.map(name => [name, model.getObjectByName(name)]));

    model.updateMatrixWorld(true);
    for (const node of nodes.values()) {
        if (node && node !== model && node.parent !== model) model.attach(node);
    }
    return {
        model,
        missing: [...nodes].filter(([, node]) => !node).map(([name]) => name),
        apply(parts: ArmPart[]) {
            for (const part of parts) {
                const node = nodes.get(part.modelNode);
                if (!node) continue;
                node.position.set(part.position.x, part.position.y, part.position.z);
                // Backend Z-up maps to scene Y-up.
                node.rotation.set(0, part.rotationDegreesZ * Math.PI / 180, 0);
            }
            model.updateMatrixWorld(true);
        },
    };
}
