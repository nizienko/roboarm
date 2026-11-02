import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { GLTFLoader } from 'three/addons/loaders/GLTFLoader.js';
import { Group, Quaternion, Vector3 } from 'three';
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

test('world poses replace GLB positions without offsets, references or axis conversion', async () => {
    const scene = await loadModel();
    const target = part('Gripper', { x: 5, y: 6, z: -7 });
    const rig = prepareArmModel(scene.clone(true), ['Gripper']);
    const scale = rig.model.getObjectByName('Gripper').scale.clone();
    rig.apply([target]);
    close(position(rig.model, 'Gripper'), new Vector3(5, 6, -7));
    close(rig.model.getObjectByName('Gripper').scale, scale);
    rig.apply([target]);
    close(position(rig.model, 'Gripper'), new Vector3(5, 6, -7));
    assert.notDeepEqual(position(scene, 'Gripper').toArray(), [5, 6, -7]);
});

test('rendering does not infer a robot hierarchy or move other nodes with a parent', async () => {
    const scene = await loadModel();
    const nodes = ['Mast', 'Lift', 'Cran', 'Gripper', 'Hand'];
    const rig = prepareArmModel(scene, nodes);
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
    const rig = prepareArmModel(scene, ['joint']);
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
    parent.attach(scene.getObjectByName('Hand'));
    const rig = prepareArmModel(scene, ['Hand']);
    rig.apply([part('Hand', { x: 0.1, y: 0.2, z: 0.3 })]);
    close(position(scene, 'Hand'), new Vector3(0.1, 0.2, 0.3));
    assert.equal(scene.getObjectByName('Hand').parent, scene);
});

test('toggle readings and targets never influence the rendered pose', async () => {
    const scene = await loadModel();
    const target = part('Lift', { x: 1, y: 2, z: 3 });
    target.toggles = [{ name: 'Height', value: 100, targetValue: 200, minValue: 0, maxValue: 300, unit: 'meters' }];
    const rig = prepareArmModel(scene, ['Lift']);
    rig.apply([target]);
    close(position(scene, 'Lift'), new Vector3(1, 2, 3));
});

test('unknown nodes are reported without blocking known parts', async () => {
    const scene = await loadModel();
    const rig = prepareArmModel(scene, ['Hand', 'NewPart']);
    assert.deepEqual(rig.missing, ['NewPart']);
    assert.doesNotThrow(() => rig.apply([part('NewPart'), part('Hand')]));
});
