import assert from 'node:assert/strict';

const base = 'http://localhost:8080';
const health = await fetch(`${base}/health`);
assert.equal(health.status, 200);
assert.deepEqual(await health.json(), { status: 'ok' });
console.log('GET /health -> 200 {"status":"ok"}');

const latest = await fetch(`${base}/signals/latest`);
assert.equal(latest.status, 200);
assert.equal(typeof (await latest.json()).speedKmh, 'number');
console.log('GET /signals/latest -> 200, simulator is producing signals');

const socket = new WebSocket('ws://localhost:8080/alerts/live');
const signal = { speedKmh: 145, batteryPercent: 10, outsideTemperatureC: -2, position: { latitude: 52.42, longitude: 10.79 } };
let ready;
const subscriptionReady = new Promise((resolve, reject) => {
    ready = resolve;
    socket.addEventListener('error', reject);
});
let delivered;
const received = new Promise(resolve => { delivered = resolve; });
socket.addEventListener('message', event => {
    const data = JSON.parse(event.data);
    if (data.status === 'ready') ready();
    if (data.rule === 'SPEED_LIMIT' && data.signal.speedKmh === 145) delivered(data);
});
const timeout = new Promise((_, reject) => setTimeout(() => reject(new Error('Smoke check timed out')), 5000).unref());
try {
    await Promise.race([subscriptionReady, timeout]);
    console.log('WebSocket /alerts/live -> ready');
    const post = await fetch(`${base}/signals`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(signal) });
    assert.equal(post.status, 201);
    const alerts = await post.json();
    assert.deepEqual(alerts.map(alert => alert.rule), ['SPEED_LIMIT', 'LOW_BATTERY', 'ICE_WARNING']);
    console.log('POST /signals -> 201, all three rule alerts returned');
    const streamed = await Promise.race([received, timeout]);
    assert.deepEqual(streamed.signal, signal);
    console.log('WebSocket /alerts/live -> matching manual alert received');
    const history = await fetch(`${base}/alerts`);
    assert.equal(history.status, 200);
    assert((await history.json()).some(alert => alert.rule === 'LOW_BATTERY' && alert.signal.speedKmh === 145));
    console.log('GET /alerts -> 200, manual alerts stored');
    const invalid = await fetch(`${base}/signals`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ...signal, batteryPercent: 101 }) });
    assert.equal(invalid.status, 400);
    assert.match((await invalid.json()).error, /batteryPercent/);
    console.log('POST /signals with battery 101 -> 400 with clear error');
    const malformed = await fetch(`${base}/signals`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{' });
    assert.equal(malformed.status, 400);
    console.log('POST /signals with malformed JSON -> 400');
    console.log('Local HTTP and WebSocket smoke checks PASSED');
} finally {
    socket.close();
}
