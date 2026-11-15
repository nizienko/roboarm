import { BoxGeometry, Mesh, MeshStandardMaterial, Object3D } from 'three';
import type { Material } from 'three';
import type { ArmPart } from './ArmState';

/** Each controlled node is rendered independently using its server-provided world pose. */
export function prepareArmModel(model: Object3D) {
    const nodes = new Map<string, Object3D>();
    let metal: Material | undefined;
    model.traverse(object => {
        if (object.name && !nodes.has(object.name)) nodes.set(object.name, object);
        if (!(object instanceof Mesh)) return;
        const materials: Material[] = Array.isArray(object.material) ? object.material : [object.material];
        metal ??= materials.find(material => material.name === 'metal');
    });
    const beams = new Map<string, Mesh<BoxGeometry, Material>>();

    return {
        model,
        missingNodes(parts: ArmPart[]) {
            return parts.filter(part => !nodes.has(part.modelNode)).map(part => part.modelNode);
        },
        apply(parts: ArmPart[]) {
            model.updateMatrixWorld(true);
            for (const part of parts) {
                const node = nodes.get(part.modelNode);
                if (node && node !== model && node.parent !== model) model.attach(node);
            }
            for (const beam of beams.values()) beam.visible = false;
            for (const part of parts) {
                const node = nodes.get(part.modelNode);
                if (!node) continue;
                node.position.set(part.position.x, part.position.y, part.position.z);
                // Backend Z-up maps to scene Y-up.
                node.rotation.set(0, part.rotationDegreesZ * Math.PI / 180, 0);
                if (part.beam) {
                    let beam = beams.get(part.modelNode);
                    if (!beam) {
                        beam = new Mesh(new BoxGeometry(1, 1, 1), metal?.clone() ?? new MeshStandardMaterial({
                            color: '#b7c0b5', metalness: 0.5, roughness: 0.35,
                        }));
                        beam.name = `${part.modelNode}Beam`;
                        node.add(beam);
                        beams.set(part.modelNode, beam);
                    }
                    // Horizontal link from this joint axis to the next axis in local +X.
                    // The solid beam overlaps the fixed motor housings at its ends.
                    const { length, width, height } = part.beam;
                    beam.position.set(length / 2, 0, 0);
                    beam.scale.set(length, height, width);
                    beam.visible = true;
                }
            }
            model.updateMatrixWorld(true);
        },
        dispose() {
            for (const beam of beams.values()) {
                beam.removeFromParent();
                beam.geometry.dispose();
                beam.material.dispose();
            }
            beams.clear();
        },
    };
}
