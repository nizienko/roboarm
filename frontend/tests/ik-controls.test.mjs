import test from 'node:test';
import assert from 'node:assert/strict';
import { clampTarget, getTargetBounds } from '../src/arm/TargetBounds.ts';

const state = {
    time: 1,
    parts: [
        { modelNode: 'Lift', beam: { length: 1, width: 0.2, height: 0.2 },
            toggles: [{ name: 'Height', unit: 'meters', value: 1.5, minValue: 0.65, maxValue: 2.7 }] },
        { modelNode: 'Elbow', beam: { length: 1, width: 0.2, height: 0.2 }, toggles: [] },
        { modelNode: 'Wrist', position: { x: 2, y: 1.4, z: 0 }, toggles: [] },
    ],
};

test('bounds require lift telemetry, wrist position and beam lengths', () => {
    assert.equal(getTargetBounds(), undefined);
    assert.equal(getTargetBounds({ time: 1, parts: [] }), undefined);
    for (const missing of ['Lift', 'Wrist']) {
        assert.equal(getTargetBounds({ ...state, parts: state.parts.filter(p => p.modelNode !== missing) }), undefined);
    }
    assert.equal(getTargetBounds({ ...state, parts: state.parts.map(p => ({ ...p, beam: null })) }), undefined);
});

test('Z bounds use the wrist world height and lift travel', () => {
    const bounds = getTargetBounds(state);
    assert.deepEqual(bounds.x, { min: -2, max: 2 });
    assert.deepEqual(bounds.y, { min: -2, max: 2 });
    assert.ok(Math.abs(bounds.z.min - 0.55) < 1e-9);
    assert.ok(Math.abs(bounds.z.max - 2.6) < 1e-9);
});

test('bounds follow beam lengths and clamp all coordinates after reconfiguration', () => {
    for (const [first, second] of [[1.5, 0.75], [0.5, 0.25]]) {
        const parts = state.parts.map(part => part.beam ? {
            ...part, beam: { ...part.beam, length: part.modelNode === 'Lift' ? first : second },
        } : part);
        const bounds = getTargetBounds({ ...state, parts });
        const reach = first + second;
        assert.deepEqual(clampTarget({ x: 9, y: -9, z: 9 }, bounds), { x: reach, y: -reach, z: bounds.z.max });
        assert.equal(clampTarget({ x: 0, y: 0, z: -9 }, bounds).z, bounds.z.min);
        const inside = { x: 0.1, y: -0.1, z: 1.5 };
        assert.deepEqual(clampTarget(inside, bounds), inside);
    }
});

test('bounds do not depend on the lift label or the moving jaw', () => {
    const parts = state.parts.map(part => part.modelNode === 'Lift' ? {
        ...part, toggles: [{ ...part.toggles[0], name: 'Vertical travel' }],
    } : part);
    parts.push({ modelNode: 'GripperJaw', position: { x: 4, y: 9, z: 2 }, toggles: [] });
    assert.deepEqual(getTargetBounds({ ...state, parts }), getTargetBounds(state));
});
