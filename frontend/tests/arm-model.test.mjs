import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { Box3, Group, Quaternion, Vector3 } from 'three';
import { prepareArmModel } from '../src/arm/ArmModel.ts';
import { isArmState } from '../src/arm/ArmState.ts';

const part = (modelNode, position = { x: 0, y: 0, z: 0 }, rotationDegreesZ = 0) => ({
    id: modelNode, modelNode, position, rotationDegreesZ, toggles: [],
});
async function loadModel() {
    const bytes = await readFile(new URL('../public/models/roboarm.glb', import.meta.url));
    return (await new GLTFLoader().parseAsync(bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength), '')).scene;
}
const position = (model, name) => model.getObjectByName(name).getWorldPosition(new Vector3());
const close = (actual, expected) => assert.ok(actual.distanceTo(expected) < 1e-6, `${actual.toArray()} != ${expected.toArray()}`);

test('wire validator requires finite absolute positions and Z angles', () => {
    const state = { time: 1, parts: [part('AnyNode')] };
    assert.ok(isArmState(state));
    state.parts[0].rotationDegreesZ = NaN;
    assert.equal(isArmState(state), false);
    state.parts[0].rotationDegreesZ = 0;
    state.parts[0].position.y = Infinity;
    assert.equal(isArmState(state), false);
    assert.equal(isArmState({ time: 1, parts: [{ id: 'old', modelNode: 'Mast', toggles: [] }] }), false);
});

test('wire validator accepts beam dimensions and rejects invalid dimensions', () => {
    const target = part('Lift');
    const state = { time: 1, parts: [target] };
    target.beam = { length: 1.5, width: 0.2, height: 0.15 };
    assert.ok(isArmState(state));
    assert.equal(state.parts[0].beam.length, 1.5);
    for (const dimension of ['length', 'width', 'height']) {
        for (const invalid of [0, -1, Infinity, NaN, '1', undefined]) {
            target.beam = { length: 1.5, width: 0.2, height: 0.15, [dimension]: invalid };
            assert.equal(isArmState(state), false, `${dimension}=${invalid}`);
        }
    }
    for (const invalid of [false, [], 'beam']) {
        target.beam = invalid;
        assert.equal(isArmState(state), false);
    }
    target.beam = null;
    assert.ok(isArmState(state));
});

test('world poses replace GLB positions without offsets, references or axis conversion', async () => {
    const scene = await loadModel();
    const target = part('Wrist', { x: 5, y: 6, z: -7 });
    const rig = prepareArmModel(scene.clone(true));
    const scale = rig.model.getObjectByName('Wrist').scale.clone();
    rig.apply([target]);
    close(position(rig.model, 'Wrist'), new Vector3(5, 6, -7));
    close(rig.model.getObjectByName('Wrist').scale, scale);
    rig.apply([target]);
    close(position(rig.model, 'Wrist'), new Vector3(5, 6, -7));
    assert.notDeepEqual(position(scene, 'Wrist').toArray(), [5, 6, -7]);
});

test('rendering does not infer a robot hierarchy or move other nodes with a parent', async () => {
    const scene = await loadModel();
    const nodes = ['Mast', 'Lift', 'Elbow', 'Wrist', 'GripperJaw'];
    const rig = prepareArmModel(scene);
    const state = nodes.map((name, i) => part(name, { x: i, y: 2, z: 3 }));
    rig.apply(state);
    const q = new Quaternion().setFromAxisAngle(new Vector3(0, 1, 0), Math.PI / 2);
    state[0].position = { x: 9, y: 8, z: 7 };
    state[0].rotationDegreesZ = 90;
    rig.apply(state);
    for (const name of nodes.slice(1)) {
        const expected = state.find(p => p.modelNode === name).position;
        close(position(scene, name), new Vector3(expected.x, expected.y, expected.z));
    }
    assert.ok(scene.getObjectByName('Mast').quaternion.angleTo(q) < 1e-6);
});

test('Z angles map to scene Y and replace the previous rotation', () => {
    const scene = new Group();
    const node = new Group();
    node.name = 'joint';
    node.rotation.set(0.3, 0.4, 0.5);
    scene.add(node);
    const rig = prepareArmModel(scene);
    for (const angle of [90, -90, 180, 360, 720, 0]) {
        rig.apply([part('joint', undefined, angle)]);
        const radians = angle * Math.PI / 180;
        const direction = new Vector3(1, 0, 0).applyQuaternion(node.quaternion);
        close(direction, new Vector3(Math.cos(radians), 0, -Math.sin(radians)));
    }
});

test('nested asset transforms do not override independent backend world poses', async () => {
    const scene = await loadModel();
    const parent = new Group();
    parent.position.set(3, 4, 5);
    parent.scale.setScalar(2);
    scene.add(parent);
    parent.attach(scene.getObjectByName('GripperJaw'));
    const rig = prepareArmModel(scene);
    rig.apply([part('GripperJaw', { x: 0.1, y: 0.2, z: 0.3 })]);
    close(position(scene, 'GripperJaw'), new Vector3(0.1, 0.2, 0.3));
    assert.equal(scene.getObjectByName('GripperJaw').parent, scene);
});

test('toggle readings and targets never influence the rendered pose', async () => {
    const scene = await loadModel();
    const target = part('Lift', { x: 1, y: 2, z: 3 });
    target.toggles = [{ name: 'Height', value: 100, targetValue: 200, minValue: 0, maxValue: 300, unit: 'meters' }];
    const rig = prepareArmModel(scene);
    rig.apply([target]);
    close(position(scene, 'Lift'), new Vector3(1, 2, 3));
});

test('unknown nodes are reported without blocking known parts', async () => {
    const scene = await loadModel();
    const rig = prepareArmModel(scene);
    assert.deepEqual(rig.missingNodes([part('GripperJaw'), part('NewPart')]), ['NewPart']);
    assert.doesNotThrow(() => rig.apply([part('NewPart'), part('GripperJaw')]));
});

test('later telemetry can control another node without rebuilding the rig', async () => {
    const rig = prepareArmModel(await loadModel());
    const lift = { ...part('Lift'), beam: { length: 1, width: 0.2, height: 0.2 } };
    rig.apply([lift]);
    const beam = rig.model.getObjectByName('LiftBeam');
    rig.apply([lift, part('Wrist', { x: 3, y: 2, z: -1 })]);
    assert.equal(rig.model.getObjectByName('LiftBeam'), beam);
    close(position(rig.model, 'Wrist'), new Vector3(3, 2, -1));
    rig.dispose();
});

test('procedural beams use server dimensions and stay horizontal through staggered, rotated joints', async () => {
    for (const [upperLength, forearmLength] of [[1, 1], [1.5, 0.75]]) {
        const scene = await loadModel();
        const rig = prepareArmModel(scene.clone(true));
        const lift = rig.model.getObjectByName('Lift');
        const elbow = rig.model.getObjectByName('Elbow');
        const motorScale = lift.scale.clone();
        const motorGeometry = lift.geometry;
        rig.apply([
            { ...part('Lift', { x: 0, y: 1.7, z: 0 }, 90),
                beam: { length: upperLength, width: 0.18, height: 0.2 } },
            { ...part('Elbow', { x: 0, y: 1.45, z: -upperLength }),
                beam: { length: forearmLength, width: 0.14, height: 0.12 } },
        ]);
        const upper = lift.getObjectByName('LiftBeam');
        const forearm = elbow.getObjectByName('ElbowBeam');
        close(upper.localToWorld(new Vector3(-0.5, 0, 0)), new Vector3(0, 1.7, 0));
        close(upper.localToWorld(new Vector3(0.5, 0, 0)), new Vector3(0, 1.7, -upperLength));
        close(forearm.localToWorld(new Vector3(-0.5, 0, 0)), new Vector3(0, 1.45, -upperLength));
        close(forearm.localToWorld(new Vector3(0.5, 0, 0)), new Vector3(forearmLength, 1.45, -upperLength));
        close(new Box3().setFromObject(upper).getSize(new Vector3()), new Vector3(0.18, 0.2, upperLength));
        close(new Box3().setFromObject(forearm).getSize(new Vector3()), new Vector3(forearmLength, 0.12, 0.14));
        close(lift.scale, motorScale);
        assert.equal(lift.geometry, motorGeometry);
        assert.equal(scene.getObjectByName('LiftBeam'), undefined);
        rig.dispose();
    }
});

test('beam updates reuse resources and cleanup preserves the cached GLB', async () => {
    const scene = await loadModel();
    const rig = prepareArmModel(scene.clone(true));
    const target = { ...part('Lift'), beam: { length: 1, width: 0.2, height: 0.2 } };
    rig.apply([target]);
    const beam = rig.model.getObjectByName('LiftBeam');
    const geometry = beam.geometry;
    const material = beam.material;
    const cachedMetal = scene.getObjectByName('Mast').material;
    assert.notEqual(material, cachedMetal);
    assert.ok(material.color.equals(cachedMetal.color));
    let disposedGeometry = 0;
    let disposedMaterial = 0;
    let disposedCachedMetal = 0;
    geometry.addEventListener('dispose', () => disposedGeometry++);
    material.addEventListener('dispose', () => disposedMaterial++);
    cachedMetal.addEventListener('dispose', () => disposedCachedMetal++);

    target.beam = { length: 1.5, width: 0.18, height: 0.16 };
    target.rotationDegreesZ = 180;
    for (let i = 0; i < 10; i++) rig.apply([target]);
    assert.equal(rig.model.getObjectByName('LiftBeam'), beam);
    assert.equal(beam.geometry, geometry);
    assert.equal(beam.material, material);
    close(beam.localToWorld(new Vector3(0.5, 0, 0)), new Vector3(-1.5, 0, 0));

    rig.apply([{ ...target, beam: null }]);
    assert.equal(beam.visible, false);
    rig.apply([target]);
    assert.equal(beam.visible, true);
    rig.apply([]);
    assert.equal(beam.visible, false);
    rig.dispose();
    assert.equal(rig.model.getObjectByName('LiftBeam'), undefined);
    assert.equal(disposedGeometry, 1);
    assert.equal(disposedMaterial, 1);
    assert.equal(disposedCachedMetal, 0);

    // React StrictMode can run effect cleanup and then mount the same rig again.
    rig.apply([target]);
    assert.notEqual(rig.model.getObjectByName('LiftBeam'), beam);
    rig.dispose();
});
