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

var findRanges = [];

function findCompile(q, regex, matchCase, wholeWord) {
  var src = regex ? q : q.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  if (wholeWord && !regex) src = "(?<![\\p{L}\\p{N}_])" + src + "(?![\\p{L}\\p{N}_])";
  return new RegExp(src, (matchCase ? "g" : "gi") + (regex ? "" : "u"));
}

function findVisible(el) {
  try {
    return el.checkVisibility ? el.checkVisibility({ checkVisibilityCSS: true }) : el.offsetParent !== null;
  } catch (e) { return true; }
}

function findScan(m) {
  findRanges = [];
  var out = { n: 0, y: [], items: [], err: false };
  var q = String(m.q || "");
  if (!q || !document.body) return out;
  var re;
  try { re = findCompile(q, !!m.regex, !!m.mc, !!m.ww); } catch (e) { out.err = true; return out; }
  var maxY = m.maxY || 1000, maxItems = m.maxItems || 200, cap = 5000;
  var height = Math.max(document.documentElement.scrollHeight, 1);
  var top = window.scrollY || 0;
  var visibility = new Map();
  var walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, {
    acceptNode: function (n) {
      var p = n.parentElement;
      if (!p || !/\S/.test(n.nodeValue)) return NodeFilter.FILTER_REJECT;
      var tag = p.tagName;
      if (tag === "SCRIPT" || tag === "STYLE" || tag === "NOSCRIPT" || tag === "TEXTAREA" || tag === "TEMPLATE") return NodeFilter.FILTER_REJECT;
      var v = visibility.get(p);
      if (v === undefined) { v = findVisible(p); visibility.set(p, v); }
      return v ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_REJECT;
    }
  });
  var node;
  while (out.n < cap && (node = walker.nextNode())) {
    var text = node.nodeValue, mt;
    re.lastIndex = 0;
    while (out.n < cap && (mt = re.exec(text)) !== null) {
      if (mt[0].length === 0) { re.lastIndex++; continue; }
      var idx = out.n++;
      var range = document.createRange();
      range.setStart(node, mt.index);
      range.setEnd(node, mt.index + mt[0].length);
      findRanges.push(range);
      if (idx < maxY) out.y.push(Math.min(1, Math.max(0, (range.getBoundingClientRect().top + top) / height)));
      if (idx < maxItems) {
        var s = Math.max(0, mt.index - 40), e = mt.index + mt[0].length;
        out.items.push({
          b: (s > 0 ? "…" : "") + text.slice(s, mt.index).replace(/\s+/g, " "),
          m: mt[0].replace(/\s+/g, " "),
          a: text.slice(e, e + 60).replace(/\s+/g, " ") + (e + 60 < text.length ? "…" : "")
        });
      }
    }
  }
  return out;
}

function findGo(i) {
  var r = findRanges[i];
  if (!r) return false;
  try {
    var sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(r);
    var el = r.startContainer.parentElement;
    if (el) el.scrollIntoView({ block: "center", inline: "nearest" });
    return true;
  } catch (e) { return false; }
}

var lastLink = { t: "", h: "", a: null };

function cleanText(v) {
  return String(v || "").trim().replace(/\s+/g, " ").substring(0, 200);
}

function urlLike(x) {
  return /^(https?:\/\/|www\.)/i.test(x) || x.indexOf("\u203a") >= 0;
}

function bestText(list) {
  var best = "";
  for (var i = 0; i < list.length; i++) {
    var x = list[i];
    if (!x) continue;
    if (!best || (urlLike(best) && !urlLike(x)) || (urlLike(best) === urlLike(x) && x.length > best.length)) best = x;
  }
  return best;
}

function pickFrom(a, sel) {
  try {
    var nodes = a.querySelectorAll(sel);
    var texts = [];
    for (var i = 0; i < nodes.length; i++) texts.push(cleanText(nodes[i].innerText || nodes[i].textContent));
    var t = bestText(texts);
    return t && !urlLike(t) ? t : "";
  } catch (e) { return ""; }
}

function linkLabel(a) {
  var t = pickFrom(a, "h1, h2, h3, h4, h5, h6, [role='heading']");
  if (!t) t = pickFrom(a, "[class*='title' i]");
  if (!t) t = cleanText(a.getAttribute("aria-label") || a.getAttribute("title"));
  if (!t) t = bestText(String(a.innerText || a.textContent || "").split("\n").map(cleanText));
  return t;
}

function trackLink(e) {
  try {
    var el = e.target;
    while (el && el.nodeType !== 1) el = el.parentNode;
    var a = el && el.closest ? el.closest("a[href], area[href]") : null;
    if (!a) return;
    if (e.type !== "touchstart" && lastLink.a === a) return;
    lastLink = { t: linkLabel(a), h: a.href || "", a: a };
  } catch (err) {}
}

try {
  document.addEventListener("touchstart", trackLink, { capture: true, passive: true });
  document.addEventListener("mousedown", trackLink, true);
  document.addEventListener("contextmenu", trackLink, true);
} catch (e) {}

var CFX_OVERLAY = "__clint_cfx";
var CFX_FILTERS = { grayscale: "grayscale(1)", sepia: "sepia(1)", black_white: "grayscale(1) contrast(1000%)" };
var cfxCfg = null;
var cfxKey = "";
var cfxApplied = "";
var cfxFor = "";
var cfxObserver = null;
var cfxRoot = null;

function cfxChannel(v) {
  var n = Number(v);
  if (!(n >= 0)) n = 0;
  if (n > 1) n = 1;
  return Math.round(n * 255);
}

function cfxBuildOverlay(cfg) {
  var t = cfg.t || [0.5, 0.5, 0.5];
  var el = document.createElement("div");
  el.setAttribute("id", CFX_OVERLAY);
  el.setAttribute("aria-hidden", "true");
  el.setAttribute("data-k", cfxKey);
  var props = [
    ["position", "fixed"], ["inset", "0"], ["top", "0"], ["left", "0"], ["right", "0"], ["bottom", "0"],
    ["width", "auto"], ["height", "auto"], ["margin", "0"], ["padding", "0"], ["border", "0"],
    ["z-index", "2147483647"], ["pointer-events", "none"], ["display", "block"], ["opacity", "1"],
    ["background", "rgb(" + cfxChannel(t[0]) + "," + cfxChannel(t[1]) + "," + cfxChannel(t[2]) + ")"],
    ["mix-blend-mode", "color"]
  ];
  props.forEach(function (p) { el.style.setProperty(p[0], p[1], "important"); });
  return el;
}

function cfxRemoveOverlay() {
  var el = document.getElementById(CFX_OVERLAY);
  if (el) el.remove();
}

function cfxClearFilter(root) {
  if (root && cfxApplied && root.style.getPropertyValue("filter") === cfxApplied) root.style.removeProperty("filter");
  cfxApplied = "";
  cfxFor = "";
}

function cfxEnsure() {
  var root = document.documentElement;
  if (!root || !cfxCfg) return;
  if (root !== cfxRoot && cfxObserver) {
    cfxRoot = root;
    cfxObserver.observe(root, { attributes: true, attributeFilter: ["style"], childList: true });
  }
  if (cfxCfg.m === "monochrome") {
    cfxClearFilter(root);
    var el = document.getElementById(CFX_OVERLAY);
    if (el && el.getAttribute("data-k") !== cfxKey) {
      el.remove();
      el = null;
    }
    if (!el) el = cfxBuildOverlay(cfxCfg);
    if (el.parentNode !== root) root.appendChild(el);
    return;
  }
  cfxRemoveOverlay();
  var value = CFX_FILTERS[cfxCfg.m];
  if (!value) return;
  if (cfxFor === value && cfxApplied && root.style.getPropertyValue("filter") === cfxApplied && root.style.getPropertyPriority("filter") === "important") return;
  root.style.setProperty("filter", value, "important");
  cfxApplied = root.style.getPropertyValue("filter");
  cfxFor = value;
}

function cfxClear() {
  if (cfxObserver) {
    cfxObserver.disconnect();
    cfxObserver = null;
  }
  cfxRoot = null;
  cfxRemoveOverlay();
  cfxClearFilter(document.documentElement);
}

function cfxWatch() {
  if (cfxObserver) return;
  cfxObserver = new MutationObserver(function () { cfxEnsure(); });
  cfxObserver.observe(document, { childList: true });
  cfxRoot = null;
  document.addEventListener("DOMContentLoaded", function () { if (cfxCfg) cfxEnsure(); });
}

function cfxSet(cfg) {
  if (!isTop) return;
  if (!cfg || !cfg.m) {
    cfxCfg = null;
    cfxKey = "";
    cfxClear();
    return;
  }
  var key = cfg.m + ":" + (cfg.t || []).join(",");
  if (cfxCfg && key === cfxKey) return;
  cfxCfg = cfg;
  cfxKey = key;
  cfxFor = "";
  cfxWatch();
  cfxEnsure();
}

function onNative(m) {
  if (!m) return;
  if (m.k === "linkText") {
    var cur = "";
    try { cur = lastLink.a ? lastLink.a.href || "" : ""; } catch (e) {}
    if (m.id) post({ k: "evalRes", id: m.id, v: ser({ t: lastLink.t, h: lastLink.h, c: cur }) });
    return;
  }
  if (m.k === "find") {
    var fo = { n: 0, y: [], items: [], err: true };
    try { fo = findScan(m); } catch (e) {}
    if (m.id) post({ k: "evalRes", id: m.id, v: JSON.stringify(fo) });
    return;
  }
  if (m.k === "findGo") {
    var ok = false;
    try { ok = findGo(m.i); } catch (e) {}
    if (m.id) post({ k: "evalRes", id: m.id, v: ok ? "1" : "0" });
    return;
  }
  if (m.k === "findClear") {
    findRanges = [];
    try { window.getSelection().removeAllRanges(); } catch (e) {}
    return;
  }
  if (m.k === "videoSize") {
    var vs = "0,0";
    try { vs = videoSize(); } catch (e) {}
    if (m.id) post({ k: "evalRes", id: m.id, v: ser(vs) });
    return;
  }
  if (m.k === "mediaScan") {
    try { mediaScan(m); } catch (e) {}
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
  if (m.k === "cfx") {
    try { cfxSet(m.cfx); } catch (e) {}
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

function connect(attempt) {
  try {
    var p = browser.runtime.connectNative("clint");
    port = p;
    p.onMessage.addListener(onNative);
    p.onDisconnect.addListener(function () {
      if (port === p) port = null;
      if (p.error && attempt < 40) setTimeout(function () { connect(attempt + 1); }, 250);
    });
  } catch (e) {
    if (attempt < 40) setTimeout(function () { connect(attempt + 1); }, 250);
  }
}
connect(0);

function sendNonce(nonce, attempt) {
  post({ k: "nonce", n: nonce }).catch(function () {
    if (attempt < 40) setTimeout(function () { sendNonce(nonce, attempt + 1); }, 250);
  });
}

function sendWho(nonce, attempt) {
  var retry = function () {
    if (attempt < 40) setTimeout(function () { sendWho(nonce, attempt + 1); }, 250);
  };
  try { browser.runtime.sendMessage({ k: "who", n: nonce }).catch(retry); } catch (e) { retry(); }
}

if (isTop) {
  var nonce = Math.random().toString(36).slice(2) + Date.now().toString(36);
  sendNonce(nonce, 0);
  sendWho(nonce, 0);
}

CFG.scripts.forEach(function (s) {
  if (s.top && !isTop) return;
  pageEval(s.c);
});

try { if (CFG.cfx) cfxSet(CFG.cfx); } catch (e) {}
