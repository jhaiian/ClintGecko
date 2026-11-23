(function () {
  if (window.__clintVt) return;
  var B = window.ClintVideoBridge;
  if (!B) return;
  var isTop = window === window.top;
  var TAKE_SITE = true;
  var entering = false, enterTimer = 0;
  var rid = Math.random().toString(36).slice(2);
  var seq = 0, cand = null, candId = "", act = null, lastAct = null, pinned = false, saved = null, fit = "fit", timer = 0, scrollTimer = 0, scrolling = false, last = "";
  var frames = {};
  var want = "play", resumeTimer = 0;
  var nativePlay = HTMLMediaElement.prototype.play;
  var nativePause = HTMLMediaElement.prototype.pause;
  var CSS = "video[data-clint-vt]{position:fixed!important;left:0!important;top:0!important;right:0!important;bottom:0!important;width:100%!important;height:100%!important;min-width:0!important;min-height:0!important;max-width:none!important;max-height:none!important;margin:0!important;padding:0!important;box-sizing:border-box!important;transform:none!important;z-index:2147483647!important;background:#000!important;object-fit:contain!important;visibility:visible!important;opacity:1!important}" +
    "video[data-clint-vt=fill]{object-fit:fill!important}" +
    "video[data-clint-vt=cover]{object-fit:cover!important}" +
    "video[data-clint-vt=none]{object-fit:none!important}" +
    "video[data-clint-vt=r169]{object-fit:fill!important;padding:max(0px,calc((var(--cvt-h,100vh) - var(--cvt-w,100vw) * 0.5625)/2)) max(0px,calc((var(--cvt-w,100vw) - var(--cvt-h,100vh) * 1.7778)/2))!important}" +
    "video[data-clint-vt=r43]{object-fit:fill!important;padding:max(0px,calc((var(--cvt-h,100vh) - var(--cvt-w,100vw) * 0.75)/2)) max(0px,calc((var(--cvt-w,100vw) - var(--cvt-h,100vh) * 1.3333)/2))!important}" +
    "html[data-clint-vt-pin] video[data-clint-vt]{left:var(--cvt-l,0px)!important;top:var(--cvt-t,0px)!important;width:var(--cvt-w,100%)!important;height:var(--cvt-h,100%)!important;right:auto!important;bottom:auto!important}" +
    "html[data-clint-vt-pin],html[data-clint-vt-pin] body{background:#000!important}" +
    "html[data-clint-vt-solo] *{visibility:hidden!important}" +
    "html[data-clint-vt-solo] video[data-clint-vt]{visibility:visible!important}";
  var sheet = null, styleEl = null;

  function ensureCss() {
    try {
      if (!sheet) {
        sheet = new CSSStyleSheet();
        sheet.replaceSync(CSS);
      }
      var list = Array.prototype.slice.call(document.adoptedStyleSheets);
      if (list.indexOf(sheet) < 0) document.adoptedStyleSheets = list.concat([sheet]);
      return;
    } catch (e) {}
    try {
      if (styleEl && styleEl.isConnected) return;
      styleEl = document.createElement("style");
      styleEl.textContent = CSS;
      (document.head || document.documentElement).appendChild(styleEl);
    } catch (e) {}
  }

  function call(m, a) {
    try { B[m](a); } catch (e) {}
  }

  function fsEl() {
    return document.fullscreenElement || document.mozFullScreenElement || null;
  }

  function visible(v) {
    var r = v.getBoundingClientRect();
    var w = Math.min(r.right, window.innerWidth) - Math.max(r.left, 0);
    var h = Math.min(r.bottom, window.innerHeight) - Math.max(r.top, 0);
    return w > 0 && h > 0 ? w * h : 0;
  }

  function pick() {
    var list = document.getElementsByTagName("video"), best = null, ba = 0;
    for (var i = 0; i < list.length; i++) {
      var v = list[i];
      if ((v.muted && v.loop) || v.readyState < 1) continue;
      if (v.paused && !(v.currentTime > 0)) continue;
      var r = v.getBoundingClientRect();
      if (r.width < 160 || r.height < 90) continue;
      var a = visible(v);
      if (!v.paused) a *= 4;
      if (a > ba) { best = v; ba = a; }
    }
    return best;
  }

  function toDevice(l, t, r, b) {
    var vv = window.visualViewport, s = vv ? vv.scale : 1, ox = vv ? vv.offsetLeft : 0, oy = vv ? vv.offsetTop : 0;
    var d = (window.devicePixelRatio || 1) * s;
    return [Math.round((l - ox) * d), Math.round((t - oy) * d), Math.round((r - ox) * d), Math.round((b - oy) * d)];
  }

  function frameRect(win) {
    var list = document.getElementsByTagName("iframe");
    for (var i = 0; i < list.length; i++) {
      if (list[i].contentWindow === win) return list[i].getBoundingClientRect();
    }
    return null;
  }

  function own() {
    var v = act ? null : pick();
    if (v !== cand) { cand = v; candId = v ? rid + "-" + (++seq) : ""; }
    if (!v) return null;
    var r = v.getBoundingClientRect();
    return { id: candId, r: [r.left, r.top, r.right, r.bottom] };
  }

  function report() {
    var mine = own();
    if (!isTop) {
      var s = JSON.stringify(mine);
      if (s !== last && window.parent === window.top) {
        last = s;
        try { window.top.postMessage({ __clintVt: rid, c: mine }, "*"); } catch (e) {}
      }
      return;
    }
    var out = [];
    if (mine) out.push({ id: mine.id, r: toDevice(mine.r[0], mine.r[1], mine.r[2], mine.r[3]) });
    for (var k in frames) {
      var f = frames[k], fr = frameRect(f.w);
      if (!fr) { delete frames[k]; continue; }
      if (!f.c) continue;
      var c = f.c.r;
      out.push({ id: f.c.id, r: toDevice(fr.left + c[0], fr.top + c[1], fr.left + c[2], fr.top + c[3]) });
    }
    var json = scrolling || fsEl() ? "[]" : JSON.stringify(out);
    if (json !== last) { last = json; call("candidates", json); }
  }

  var pending = 0;
  function schedule() {
    if (pending) return;
    pending = setTimeout(function () { pending = 0; report(); }, 150);
  }

  function owner() {
    var f = fsEl();
    if (!f) return null;
    if (f.tagName === "VIDEO") return f;
    var whole = f === document.documentElement || f === document.body;
    var min = window.innerWidth * window.innerHeight / 4;
    var list = f.getElementsByTagName("video"), best = null, bs = -1;
    for (var i = 0; i < list.length; i++) {
      var v = list[i], r = v.getBoundingClientRect(), s = r.width * r.height;
      if (whole && ((v.muted && v.loop) || (v.paused && !(v.currentTime > 0)) || visible(v) < min)) continue;
      if (v.videoWidth > 0) s += 1e8;
      if (!v.paused) s += 1e9;
      if (s > bs) { best = v; bs = s; }
    }
    return best;
  }

  function retarget() {
    if (!act || (act.isConnected && !act.ended && (!act.paused || want === "pause"))) return false;
    var list = document.getElementsByTagName("video");
    for (var i = 0; i < list.length; i++) {
      var v = list[i];
      if (v !== act && !v.paused && !v.ended && v.readyState > 1 && !(v.muted && v.loop)) {
        attach(v);
        return true;
      }
    }
    return false;
  }

  function push() {
    if (retarget()) return;
    var v = act;
    if (!v) return;
    fitPin();
    var d = v.duration, b = 0;
    try { if (v.buffered.length) b = v.buffered.end(v.buffered.length - 1); } catch (e) {}
    var tt = [];
    try {
      for (var i = 0; i < v.textTracks.length; i++) {
        var k = v.textTracks[i];
        if (k.kind !== "subtitles" && k.kind !== "captions") continue;
        tt.push({ i: i, l: k.label || "", g: k.language || "", on: k.mode === "showing" });
      }
    } catch (e) {}
    call("state", JSON.stringify({
      tt: tt,
      p: v.paused || v.ended,
      t: v.currentTime || 0,
      d: isFinite(d) ? d : -1,
      b: b,
      r: v.playbackRate || 1,
      w: v.videoWidth || 0,
      h: v.videoHeight || 0,
      f: fit
    }));
  }

  function attach(target) {
    var v = target || owner();
    if (!v || v === act) return;
    detach(true);
    ensureCss();
    act = v;
    lastAct = v;
    saved = { controls: v.controls };
    v.controls = false;
    want = v.paused ? "pause" : "play";
    try {
      v.pause = function () {
        if (this === act && want === "play") return;
        return nativePause.apply(this, arguments);
      };
    } catch (e) {}
    v.setAttribute("data-clint-vt", fit);
    document.documentElement.setAttribute("data-clint-vt-solo", "");
    blockExit(true);
    timer = setInterval(push, 500);
    push();
  }

  function detach(silent) {
    if (!act) return;
    var v = act;
    act = null;
    clearInterval(timer);
    clearTimeout(resumeTimer);
    try { delete v.pause; } catch (e) {}
    blockExit(false);
    v.removeAttribute("data-clint-vt");
    document.documentElement.removeAttribute("data-clint-vt-solo");
    v.controls = saved.controls;
    if (!silent) call("detached", rid);
    schedule();
  }

  function onFullscreen() {
    if (fsEl()) {
      if (TAKE_SITE || entering || act) attach();
      entering = false;
    } else setTimeout(function () { if (!pinned && !fsEl()) detach(false); }, 300);
  }

  var PIN_VARS = ["--cvt-l", "--cvt-t", "--cvt-w", "--cvt-h"];

  function fitPin() {
    if (!pinned) return;
    var vv = window.visualViewport, s = document.documentElement.style, i;
    if (!vv || !(vv.width > 0) || !(vv.height > 0)) {
      for (i = 0; i < 4; i++) s.removeProperty(PIN_VARS[i]);
      return;
    }
    var box = [Math.max(0, vv.offsetLeft), Math.max(0, vv.offsetTop), vv.width, vv.height];
    for (i = 0; i < 4; i++) s.setProperty(PIN_VARS[i], box[i] + "px");
  }

  var EXIT_NAMES = ["exitFullscreen", "mozCancelFullScreen", "webkitExitFullscreen"];

  function blockExit(on) {
    EXIT_NAMES.forEach(function (n) {
      try {
        if (on) document[n] = function () { return n === "exitFullscreen" ? Promise.resolve() : undefined; };
        else delete document[n];
      } catch (e) {}
    });
  }

  function pin() {
    var v = act || lastAct;
    if (!v || !v.isConnected) return;
    pinned = true;
    attach(v);
    fitPin();
    document.documentElement.setAttribute("data-clint-vt-pin", "");
    push();
  }

  function release() {
    pinned = false;
    lastAct = null;
    for (var i = 0; i < 4; i++) document.documentElement.style.removeProperty(PIN_VARS[i]);
    document.documentElement.removeAttribute("data-clint-vt-pin");
    if (!fsEl()) detach(true);
  }

  function enter(id) {
    if (!cand || id !== candId) return;
    var v = cand;
    entering = true;
    clearTimeout(enterTimer);
    enterTimer = setTimeout(function () { entering = false; }, 3000);
    try {
      var p = v.requestFullscreen ? v.requestFullscreen() : v.mozRequestFullScreen();
      if (p && p.catch) p.catch(function () {});
    } catch (e) {}
  }

  function cmd(c, x) {
    var v = act;
    if (!v) return;
    try {
      if (c === "play") { want = "play"; resume(true); }
      else if (c === "pause") { want = "pause"; clearTimeout(resumeTimer); nativePause.call(v); }
      else if (c === "seek") v.currentTime = Math.max(0, x);
      else if (c === "skip") v.currentTime = Math.max(0, (v.currentTime || 0) + x);
      else if (c === "rate") v.playbackRate = x;
      else if (c === "fit") { fit = String(x); v.setAttribute("data-clint-vt", fit); }
      else if (c === "sub") {
        for (var j = 0; j < v.textTracks.length; j++) {
          var t = v.textTracks[j];
          if (t.kind !== "subtitles" && t.kind !== "captions") continue;
          t.mode = j === x ? "showing" : "disabled";
        }
      }
    } catch (e) {}
    push();
  }

  function resume(force) {
    var v = act;
    if (!v || want !== "play" || !v.paused || (v.ended && force !== true)) return;
    try {
      var p = nativePlay.call(v);
      if (p && p.catch) p.catch(function () {});
    } catch (e) {}
  }

  ["play", "playing", "pause", "ended", "loadedmetadata", "emptied", "volumechange"].forEach(function (n) {
    document.addEventListener(n, function (e) {
      if (act && e.target === act) {
        if (n === "play") want = "play";
        if (n === "pause" && want === "play") {
          clearTimeout(resumeTimer);
          resumeTimer = setTimeout(resume, 250);
        }
        push();
      } else schedule();
    }, true);
  });
  ["seeked", "ratechange", "durationchange"].forEach(function (n) {
    document.addEventListener(n, function (e) { if (act && e.target === act) push(); }, true);
  });
  document.addEventListener("fullscreenchange", onFullscreen, true);
  window.addEventListener("resize", function () { fitPin(); schedule(); });
  if (window.visualViewport) {
    window.visualViewport.addEventListener("resize", fitPin);
    window.visualViewport.addEventListener("scroll", fitPin);
  }
  window.addEventListener("scroll", function () {
    if (!isTop) { schedule(); return; }
    if (!scrolling) { scrolling = true; report(); }
    clearTimeout(scrollTimer);
    scrollTimer = setTimeout(function () { scrolling = false; report(); }, 250);
  }, { capture: true, passive: true });
  if (isTop) {
    window.addEventListener("message", function (e) {
      var m = e.data;
      if (!m || typeof m !== "object" || typeof m.__clintVt !== "string" || !e.source) return;
      frames[m.__clintVt] = { w: e.source, c: m.c && typeof m.c.id === "string" && m.c.r && m.c.r.length === 4 ? m.c : null };
      schedule();
    });
  }
  setInterval(report, 1500);

  function refresh() {
    last = "";
    schedule();
  }

  window.__clintVt = { enter: enter, cmd: cmd, pin: pin, release: release, refresh: refresh };
  if (fsEl() && TAKE_SITE) attach();
  schedule();
})();
