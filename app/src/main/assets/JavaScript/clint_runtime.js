var W = window.wrappedJSObject;
var isTop = window === window.top;
var seq = 0;
var permCache = {};
var port = null;

function post(msg) {
  try { return browser.runtime.sendNativeMessage("clint", msg); } catch (e) { return Promise.reject(e); }
}

function ser(v) {
  try {
    var s = JSON.stringify(v);
    return s === undefined ? "null" : s;
  } catch (e) { return "null"; }
}

function injectElement(code) {
  var run = function () {
    var s = document.createElement("script");
    s.textContent = code;
    (document.head || document.documentElement).appendChild(s);
    s.remove();
  };
  if (document.documentElement) { run(); return; }
  var mo = new MutationObserver(function () {
    if (document.documentElement) { mo.disconnect(); run(); }
  });
  mo.observe(document, { childList: true });
}

function pageEval(code) {
  try {
    return { ok: true, v: W.eval(code) };
  } catch (e) {
    var msg = String(e && e.message ? e.message : e);
    if (/CSP|unsafe-eval|call to eval|EvalError|blocked/i.test(msg)) {
      try { injectElement(code); } catch (e2) {}
    }
    return { ok: false };
  }
}

function toArgs(list) {
  var out = [];
  for (var i = 0; i < list.length; i++) {
    var a = list[i];
    out.push(a !== null && typeof a === "object" ? String(a) : (a === undefined ? null : a));
  }
  return out;
}

function invoke(iface, method, kind, args) {
  var key = iface + "." + method;
  if (kind === "c") return CFG.snap[key] === undefined ? true : CFG.snap[key];
  if (kind === "x") {
    var h = String(args[0] || "");
    for (var i = 0; i < CFG.dsx.length; i++) {
      var d = CFG.dsx[i];
      if (h === d || h.endsWith("." + d)) return true;
    }
    return false;
  }
  if (kind === "p") {
    var ck = key + "|" + args[0];
    post({ k: "call", i: iface, m: method, a: toArgs(args) }).then(function (r) {
      if (r && r.r !== undefined) permCache[ck] = r.r;
    }, function () {});
    return permCache[ck] !== undefined ? permCache[ck] : (CFG.perm[ck] !== undefined ? CFG.perm[ck] : "default");
  }
  if (kind === "a") {
    var a = toArgs(args);
    return new W.Promise(exportFunction(function (resolve) {
      post({ k: "call", i: iface, m: method, a: a }).then(function (r) {
        resolve(r && r.r !== undefined && r.r !== null ? r.r : "");
      }, function () { resolve(""); });
    }, W));
  }
  post({ k: "call", i: iface, m: method, a: toArgs(args) });
}

function exposeInterface(spec) {
  var obj = new W.Object();
  spec.m.forEach(function (m) {
    exportFunction(function () {
      return invoke(spec.n, m.n, m.k, Array.prototype.slice.call(arguments));
    }, obj, { defineAs: m.n });
  });
  W[spec.n] = obj;
}

function shadowOf(el) {
  try { return el.openOrClosedShadowRoot || el.shadowRoot || null; } catch (e) { return null; }
}

function collectVideos(root, out) {
  if (!root || !root.querySelectorAll) return out;
  if (root.tagName === "VIDEO") out.push(root);
  var videos = root.querySelectorAll("video");
  for (var i = 0; i < videos.length; i++) out.push(videos[i]);
  var own = shadowOf(root);
  if (own) collectVideos(own, out);
  var all = root.querySelectorAll("*");
  for (var j = 0; j < all.length; j++) {
    var sr = shadowOf(all[j]);
    if (sr) collectVideos(sr, out);
  }
  return out;
}

function fullscreenTarget() {
  var el = document.fullscreenElement || document.mozFullScreenElement || null;
  while (el) {
    var sr = shadowOf(el);
    if (!sr || !sr.fullscreenElement) break;
    el = sr.fullscreenElement;
  }
  return el;
}

function visibleArea(v) {
  var r = v.getBoundingClientRect();
  var w = Math.min(r.right, window.innerWidth) - Math.max(r.left, 0);
  var h = Math.min(r.bottom, window.innerHeight) - Math.max(r.top, 0);
  return w > 0 && h > 0 ? w * h : 0;
}

function pickVideo(list) {
  var best = null, bestScore = -1, bestArea = -1;
  for (var i = 0; i < list.length; i++) {
    var v = list[i], a = visibleArea(v), s = 0;
    if (v.videoWidth > 0 && v.videoHeight > 0) s += 8;
    if (!v.paused && !v.ended) s += 4;
    if (a > 0) s += 2;
    if (s > bestScore || (s === bestScore && a > bestArea)) { best = v; bestScore = s; bestArea = a; }
  }
  return best;
}

function videoSize() {
  var fs = fullscreenTarget();
  var v = fs ? pickVideo(collectVideos(fs, [])) : null;
  if (!v || !(v.videoWidth > 0)) v = pickVideo(collectVideos(document, [])) || v;
  return v && v.videoWidth > 0 && v.videoHeight > 0 ? v.videoWidth + "," + v.videoHeight : "0,0";
}

function onNative(m) {
  if (!m) return;
  if (m.k === "videoSize") {
    var vs = "0,0";
    try { vs = videoSize(); } catch (e) {}
    if (m.id) post({ k: "evalRes", id: m.id, v: ser(vs) });
    return;
  }
  if (m.k === "reader") {
    var rout = "null";
    try {
      var art = new Readability(document.cloneNode(true)).parse();
      if (art && art.content) {
        rout = JSON.stringify({ title: art.title || document.title || "", content: art.content });
      }
    } catch (e) {}
    if (m.id) post({ k: "evalRes", id: m.id, v: rout });
    return;
  }
  if (m.k === "eval") {
    var out = "null";
    var r = pageEval(m.code);
    if (r.ok) out = ser(r.v);
    if (m.id) post({ k: "evalRes", id: m.id, v: out });
  }
}

try {
  CFG.ifaces.forEach(exposeInterface);
} catch (e) {}

try {
  port = browser.runtime.connectNative("clint");
  port.onMessage.addListener(onNative);
  port.onDisconnect.addListener(function () { port = null; });
} catch (e) {}

if (isTop) {
  var nonce = Math.random().toString(36).slice(2) + Date.now().toString(36);
  post({ k: "nonce", n: nonce });
  try { browser.runtime.sendMessage({ k: "who", n: nonce }); } catch (e) {}
}

CFG.scripts.forEach(function (s) {
  if (s.top && !isTop) return;
  pageEval(s.c);
});
