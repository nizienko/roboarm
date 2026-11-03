import test from 'node:test';
import assert from 'node:assert/strict';
import { connectArmSocket } from '../src/arm/ArmSocket.ts';

function setup(t) {
    const sockets = [];
    const original = globalThis.WebSocket;
    globalThis.WebSocket = class extends EventTarget {
        constructor(url) {
            super();
            this.url = url;
            sockets.push(this);
        }
        close() { this.dispatchEvent(new Event('close')); }
    };
    t.after(() => { globalThis.WebSocket = original; });
    t.mock.timers.enable({ apis: ['setTimeout'] });
    const state = { socket: null, opens: 0, closes: 0, messages: 0 };
    const stop = connectArmSocket('ws://localhost:6593/ws', {
        onSocket: (socket) => { state.socket = socket; },
        onOpen: () => { state.opens++; },
        onClose: () => { state.closes++; },
        onMessage: () => { state.messages++; },
    });
    t.after(stop);
    return { sockets, state, stop };
}

test('reconnects after failure, backs off and resets delay after opening', (t) => {
    const { sockets, state } = setup(t);
    sockets[0].dispatchEvent(new Event('error'));
    assert.equal(state.socket, null);
    t.mock.timers.tick(499);
    assert.equal(sockets.length, 1);
    t.mock.timers.tick(1);
    assert.equal(sockets.length, 2);
    sockets[1].close();
    t.mock.timers.tick(999);
    assert.equal(sockets.length, 2);
    t.mock.timers.tick(1);
    sockets[2].dispatchEvent(new Event('open'));
    sockets[2].dispatchEvent(new Event('message'));
    assert.equal(state.opens, 1);
    assert.equal(state.messages, 1);
    sockets[2].close();
    t.mock.timers.tick(500);
    assert.equal(sockets.length, 4);
    assert.equal(state.socket, sockets[3]);
});

test('cleanup cancels retries and ignores events from the old connection', (t) => {
    const { sockets, state, stop } = setup(t);
    sockets[0].close();
    stop();
    sockets[0].dispatchEvent(new Event('open'));
    sockets[0].dispatchEvent(new Event('message'));
    t.mock.timers.tick(10_000);
    assert.equal(sockets.length, 1);
    assert.equal(state.socket, null);
    assert.equal(state.opens, 0);
    assert.equal(state.messages, 0);
});
