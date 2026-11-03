import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { registerHooks } from 'node:module';
import { createElement } from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import ts from 'typescript';

registerHooks({
    load(url, context, nextLoad) {
        if (!url.endsWith('.tsx')) return nextLoad(url, context);
        return {
            format: 'module',
            shortCircuit: true,
            source: ts.transpileModule(readFileSync(new URL(url), 'utf8'), {
                compilerOptions: { jsx: ts.JsxEmit.ReactJSX, module: ts.ModuleKind.ESNext },
            }).outputText,
        };
    },
});
const { IKArmControls } = await import('../src/arm/IKArmControls.tsx');
const state = {
    time: 1,
    parts: [
        { modelNode: 'Lift', toggles: [{ name: 'Height', value: 1.5, minValue: 0.65, maxValue: 2.7 }] },
        { modelNode: 'Gripper', position: { x: 2, y: 1.4, z: 0 }, toggles: [] },
    ],
};
const render = (props = {}) => renderToStaticMarkup(createElement(IKArmControls, {
    state, disabled: false, onCommand() {}, ...props,
}));

test('IK controls disable the button and coordinate inputs when disconnected', () => {
    assert.match(render({ disabled: true }), /<fieldset disabled="">/);
});

test('IK controls stay disabled until lift limits arrive', () => {
    assert.match(render({ state: undefined }), /<fieldset disabled="">/);
    assert.match(render({ state: { time: 1, parts: [] } }), /<fieldset disabled="">/);
});

test('connected IK controls expose the world height range and initial coordinates', () => {
    const html = render();
    assert.doesNotMatch(html, /disabled=""/);
    assert.match(html, /id="arm-target-z"[^>]*min="0.55"[^>]*max="2.6"[^>]*value="1.5"/);
    assert.match(html, /x=2.00; y=0.00; z=1.50/);
});
