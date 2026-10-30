const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const SOURCE = fs.readFileSync(path.join(__dirname, '..', 'app/src/main/assets/clint-ext/background.js'), 'utf8');

function setup(media = true) {
  const listeners = {};
  const sent = [];
  const event = name => ({ addListener(fn) { (listeners[name] ||= []).push(fn); } });
  const port = { onMessage: event('native'), onDisconnect: event('disconnect'), postMessage() {} };
  const browser = {
    runtime: {
      connectNative: () => port,
      sendNativeMessage: (_, msg) => { sent.push(msg); return Promise.resolve(); },
      onMessage: event('message')
    },
    webRequest: Object.fromEntries(
      ['onBeforeRequest', 'onBeforeSendHeaders', 'onSendHeaders', 'onHeadersReceived', 'onCompleted', 'onErrorOccurred']
        .map(name => [name, event(name)])
    ),
    contentScripts: { register: async () => ({ unregister: async () => {} }) },
    cookies: {}
  };
  vm.runInNewContext(SOURCE, { browser, URL, Map, Promise, setTimeout() {} });
  listeners.native[0]({ k: 'cfg', cfg: { media } });
  const request = (id, url, opts = {}) => ({
    requestId: id, url, tabId: 7, frameId: 2, type: 'xmlhttprequest', method: 'GET',
    documentUrl: 'https://embed.example/player',
    frameAncestors: [{ url: 'https://embed.example/player' }, { url: 'https://page.example/watch' }],
    requestHeaders: [{ name: 'Referer', value: 'https://embed.example/player' }, { name: 'Cookie', value: 'secret' }],
    ...opts
  });
  const send = d => listeners.onSendHeaders[0](d);
  const response = (d, contentType, extra = {}) => ({
    ...d, statusCode: 200,
    responseHeaders: [{ name: 'Content-Type', value: contentType }, ...(extra.headers || [])],
    ...(extra.status ? { statusCode: extra.status } : {})
  });
  const headers = (d, contentType, extra) => listeners.onHeadersReceived[1](response(d, contentType, extra));
  const completed = (d, contentType, extra) => listeners.onCompleted[0](response(d, contentType, extra));
  return { listeners, sent, request, send, headers, completed };
}

test('direct media paths and media element requests are forwarded immediately', () => {
  const e = setup();
  e.send(e.request('1', 'https://cdn.example/video.mp4?sig=x'));
  e.send(e.request('2', 'https://cdn.example/v1.2/master.m3u8?token=x'));
  e.send(e.request('3', 'https://cdn.example/stream/42', { type: 'media' }));
  assert.equal(e.sent.length, 3);
  assert.deepEqual(e.sent.map(m => m.tab), [7, 7, 7]);
  assert.equal(e.sent[0].page, 'https://page.example/watch');
  assert.equal(e.sent[0].h.Referer, 'https://embed.example/player');
  assert.equal(Object.keys(e.sent[0].h).some(k => k.toLowerCase() === 'cookie'), false);
});

test('extensionless responses are forwarded unless they are clearly not media', () => {
  const e = setup();
  const types = {
    hls: 'application/vnd.apple.mpegurl; charset=utf-8',
    dash: 'application/dash+xml',
    plain: 'text/plain',
    s3: 'binary/octet-stream',
    vtt: 'text/vtt',
    none: '',
    json: 'application/json',
    ld: 'application/ld+json',
    html: 'text/html',
    js: 'application/javascript',
    png: 'image/png',
    font: 'font/woff2'
  };
  for (const [id, ct] of Object.entries(types)) {
    const d = e.request(id, 'https://cdn.example/play/' + id + '.php');
    e.send(d);
    e.headers(d, ct);
  }
  assert.deepEqual(e.sent.map(m => m.ct), [
    'application/vnd.apple.mpegurl', 'application/dash+xml', 'text/plain', 'binary/octet-stream', 'text/vtt', ''
  ]);
});

test('segments, ads, tiny bodies, failures and disabled capture are ignored', () => {
  const e = setup();
  for (const d of [
    e.request('a', 'https://cdn.example/seg-1.ts'),
    e.request('b', 'https://cdn.example/chunk.m4s'),
    e.request('c', 'https://ad.doubleclick.net/ad.mp4'),
    e.request('d', 'https://cdn.example/x.mp4', { method: 'POST' })
  ]) e.send(d);
  const segment = e.request('seg', 'https://cdn.example/seg?id=1');
  e.send(segment); e.headers(segment, 'video/mp2t');
  const tiny = e.request('tiny', 'https://cdn.example/ping');
  e.send(tiny); e.headers(tiny, 'text/plain', { headers: [{ name: 'Content-Length', value: '2' }] });
  const missing = e.request('404', 'https://cdn.example/missing');
  e.send(missing); e.headers(missing, 'text/plain', { status: 404 });
  const failed = e.request('failed', 'https://cdn.example/failed');
  e.send(failed); e.listeners.onErrorOccurred[0](failed); e.headers(failed, 'video/mp4');
  assert.equal(e.sent.length, 0);

  const off = setup(false);
  off.send(off.request('x', 'https://cdn.example/video.mp4'));
  assert.equal(off.sent.length, 0);
});

test('host filtering matches ad labels without catching unrelated hosts', () => {
  const e = setup();
  e.send(e.request('1', 'https://cdn.roadformula.example/movie.mp4'));
  e.send(e.request('2', 'https://cdn.adform.net/movie.mp4'));
  assert.deepEqual(e.sent.map(m => m.u), ['https://cdn.roadformula.example/movie.mp4']);
});

test('redirects, completion fallback and service worker requests are handled', () => {
  const e = setup();
  const d = e.request('r', 'https://cdn.example/go');
  e.send(d);
  e.headers(d, 'text/html', { status: 302 });
  const redirected = { ...d, url: 'https://media.example/file' };
  e.send(redirected);
  e.headers(redirected, 'video/mp4');
  const cached = e.request('c', 'https://cdn.example/cached');
  e.send(cached);
  e.completed(cached, 'audio/mpeg');
  e.headers(cached, 'audio/mpeg');
  const worker = e.request('w', 'https://cdn.example/sw-stream', { tabId: -1 });
  e.send(worker);
  e.headers(worker, 'application/x-mpegurl');
  assert.deepEqual(e.sent.map(m => [m.u, m.tab]), [
    ['https://media.example/file', 7],
    ['https://cdn.example/cached', 7],
    ['https://cdn.example/sw-stream', -1]
  ]);
});

test('pending response tracking stays bounded', () => {
  const e = setup();
  const first = e.request('first', 'https://cdn.example/first');
  e.send(first);
  for (let i = 0; i < 512; i++) e.send(e.request('id' + i, 'https://cdn.example/' + i));
  e.headers(first, 'video/mp4');
  assert.equal(e.sent.length, 0);
});
