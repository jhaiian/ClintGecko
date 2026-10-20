var mediaScan = (function () {
  var EXT = "mp4|m4v|webm|mkv|mov|3gp|3g2|flv|avi|wmv|m3u8|mpd|mp3|m4a|aac|ogg|oga|opus|flac|wav|weba|vtt|srt";
  var SUB_EXT = /\.(vtt|srt)$/i;
  var AUDIO_EXT = /\.(mp3|m4a|aac|oga|opus|flac|wav|weba)$/i;
  var PATH_RE = new RegExp("\\.(" + EXT + ")$", "i");
  var ABS_RE = new RegExp("https?://[^\\s\"'<>()\\[\\]{}|^`\\\\]+?\\.(?:" + EXT + ")(?:[?#][^\\s\"'<>()\\[\\]{}|^`\\\\]*)?(?=[\\s\"'<>()\\[\\]{}|^`\\\\,;]|$)", "gi");
  var QUOTED_RE = new RegExp("[\"']((?:\\.{0,2}/)?[^\\s\"'<>()\\[\\]{}|^`\\\\:]{1,1024}?\\.(?:" + EXT + ")(?:\\?[^\\s\"'<>\\\\]*)?)[\"']", "gi");
  var LABEL_ATTRS = ["label", "data-label", "title", "res", "data-res", "size", "data-size", "data-quality", "quality", "data-title"];
  var DATA_SELECTOR = "[data-setup],[data-config],[data-video],[data-videos],[data-sources],[data-src],[data-file],[data-url],[data-hls],[data-mp4],[data-stream],[data-source],[data-video-src],[data-video-url],[data-media],[data-mp4-url],[data-hls-url],[data-dash],[flashvars]";
  var META_SELECTOR = "meta[property^='og:video'],meta[property^='og:audio'],meta[name^='twitter:player:stream'],meta[itemprop='contentUrl'],meta[itemprop='embedUrl'],link[itemprop='contentUrl'],link[rel='preload'][as='video'],link[rel='preload'][as='audio'],link[rel='preload'][as='fetch']";
  var MAX_ITEMS = 120;
  var MAX_SCRIPT_CHARS = 4 * 1024 * 1024;
  var sent = new Set();
  var scannedScripts = new WeakSet();
  var active = false;
  var observer = null;
  var timer = 0;
  var sentCount = 0;

  function absolute(u) {
    if (!u) return null;
    var s = String(u).trim();
    if (!s || s.length > 4096) return null;
    if (/^(blob|data|javascript|mediasource|about):/i.test(s)) return null;
    try {
      var r = new URL(s, document.baseURI);
      return r.protocol === "http:" || r.protocol === "https:" ? r.href.split("#")[0] : null;
    } catch (e) { return null; }
  }

  function hasMediaPath(u) {
    try { return PATH_RE.test(new URL(u).pathname); } catch (e) { return false; }
  }

  function kindOf(u, hint) {
    if (hint) return hint;
    var p = "";
    try { p = new URL(u).pathname; } catch (e) {}
    if (SUB_EXT.test(p)) return "s";
    if (AUDIO_EXT.test(p)) return "a";
    return "";
  }

  function cleanLabel(v) {
    if (v === null || v === undefined) return "";
    var s = String(v).replace(/\s+/g, " ").trim();
    if (/^\d{3,4}$/.test(s)) s += "p";
    return s.length > 40 ? "" : s;
  }

  function add(out, raw, hint, label, lang) {
    if (sentCount + out.length >= MAX_ITEMS) return;
    var u = absolute(raw);
    if (!u || u === location.href || sent.has(u)) return;
    sent.add(u);
    out.push({ u: u, t: kindOf(u, hint), l: cleanLabel(label), g: lang ? String(lang).slice(0, 16) : "" });
  }

  function normalize(text) {
    return text
      .replace(/\\u002[fF]/g, "/")
      .replace(/\\u0026/g, "&")
      .replace(/\\u003[dD]/g, "=")
      .replace(/\\\//g, "/")
      .replace(/&amp;/g, "&");
  }

  function scanText(out, text, quoted) {
    if (!text || text.indexOf(".") < 0) return;
    var t = normalize(text);
    var m;
    ABS_RE.lastIndex = 0;
    while ((m = ABS_RE.exec(t)) !== null) add(out, m[0], "", "", "");
    if (!quoted) return;
    QUOTED_RE.lastIndex = 0;
    while ((m = QUOTED_RE.exec(t)) !== null) {
      if (m[1].indexOf("/") >= 0) add(out, m[1], "", "", "");
    }
  }

  function labelOf(el) {
    for (var i = 0; i < LABEL_ATTRS.length; i++) {
      var v = el.getAttribute(LABEL_ATTRS[i]);
      if (v) return v;
    }
    return "";
  }

  function roots(full) {
    var list = [document];
    if (!full || !document.documentElement) return list;
    var all = document.querySelectorAll("*");
    for (var i = 0; i < all.length; i++) {
      var sr = shadowOf(all[i]);
      if (sr) list.push(sr);
    }
    return list;
  }

  function scanMediaElements(out, root) {
    var media = root.querySelectorAll("video,audio");
    for (var i = 0; i < media.length; i++) {
      var el = media[i];
      var hint = el.tagName === "AUDIO" ? "a" : "";
      add(out, el.getAttribute("src"), hint, labelOf(el), "");
      add(out, el.currentSrc, hint, labelOf(el), "");
      add(out, el.getAttribute("data-src"), hint, labelOf(el), "");
    }
    var sources = root.querySelectorAll("source");
    for (var j = 0; j < sources.length; j++) {
      var s = sources[j];
      var parent = s.parentElement ? s.parentElement.tagName : "";
      if (parent === "PICTURE") continue;
      var type = (s.getAttribute("type") || "").toLowerCase();
      var sh = parent === "AUDIO" || type.indexOf("audio/") === 0 ? "a" : "";
      add(out, s.getAttribute("src"), sh, labelOf(s), "");
      add(out, s.getAttribute("data-src"), sh, labelOf(s), "");
    }
    var tracks = root.querySelectorAll("track");
    for (var k = 0; k < tracks.length; k++) {
      var tr = tracks[k];
      var kind = (tr.getAttribute("kind") || "subtitles").toLowerCase();
      if (kind !== "subtitles" && kind !== "captions") continue;
      add(out, tr.getAttribute("src"), "s", tr.getAttribute("label"), tr.getAttribute("srclang"));
    }
  }

  function scanLinks(out, root) {
    var links = root.querySelectorAll("a[href]");
    for (var i = 0; i < links.length; i++) {
      var href = absolute(links[i].getAttribute("href"));
      if (href && hasMediaPath(href)) add(out, href, "", "", "");
    }
    var metas = root.querySelectorAll(META_SELECTOR);
    for (var j = 0; j < metas.length; j++) {
      var v = metas[j].getAttribute("content") || metas[j].getAttribute("href");
      var u = absolute(v);
      if (!u) continue;
      var prop = (metas[j].getAttribute("property") || metas[j].getAttribute("name") || metas[j].getAttribute("as") || "").toLowerCase();
      if (hasMediaPath(u) || prop.indexOf("stream") >= 0 || prop === "video" || prop === "audio" ||
          prop === "og:video" || prop === "og:video:url" || prop === "og:video:secure_url" ||
          prop === "og:audio" || prop === "og:audio:url" || prop === "og:audio:secure_url") {
        add(out, u, prop.indexOf("audio") >= 0 ? "a" : "", "", "");
      }
    }
    var embeds = root.querySelectorAll("embed[src],object[data],param[value]");
    for (var k = 0; k < embeds.length; k++) {
      var e = embeds[k];
      var val = e.getAttribute("src") || e.getAttribute("data") || e.getAttribute("value") || "";
      scanText(out, val, false);
      var abs = absolute(val);
      if (abs && hasMediaPath(abs)) add(out, abs, "", "", "");
    }
  }

  function scanDataAttributes(out, root) {
    var els = root.querySelectorAll(DATA_SELECTOR);
    for (var i = 0; i < els.length && i < 500; i++) {
      var attrs = els[i].attributes;
      for (var j = 0; j < attrs.length; j++) {
        var name = attrs[j].name;
        if (name.indexOf("data-") !== 0 && name !== "flashvars") continue;
        var value = attrs[j].value;
        if (!value || value.length > 200000) continue;
        scanText(out, value, true);
        var abs = /^[\w./-]/.test(value) && value.indexOf(" ") < 0 ? absolute(value) : null;
        if (abs && hasMediaPath(abs)) add(out, abs, "", labelOf(els[i]), "");
      }
    }
  }

  function scanScripts(out) {
    var scripts = document.querySelectorAll("script:not([src])");
    var budget = MAX_SCRIPT_CHARS;
    for (var i = 0; i < scripts.length && budget > 0; i++) {
      var s = scripts[i];
      if (scannedScripts.has(s)) continue;
      var type = (s.getAttribute("type") || "").toLowerCase();
      if (type && type.indexOf("javascript") < 0 && type.indexOf("json") < 0 && type !== "module" && type.indexOf("text/x-") !== 0) continue;
      var text = s.textContent || "";
      if (!text) continue;
      scannedScripts.add(s);
      if (text.length > budget) text = text.slice(0, budget);
      budget -= text.length;
      scanText(out, text, true);
    }
  }

  function readProp(obj, name) {
    try { return obj ? obj[name] : undefined; } catch (e) { return undefined; }
  }

  function scanPlayerList(out, list) {
    var n = Math.min(readProp(list, "length") || 0, 50);
    for (var i = 0; i < n; i++) {
      var item = readProp(list, i);
      if (!item) continue;
      var file = readProp(item, "file") || readProp(item, "src") || readProp(item, "url");
      if (typeof file === "string") add(out, file, "", readProp(item, "label") || readProp(item, "res") || readProp(item, "size") || readProp(item, "quality"), "");
      var sources = readProp(item, "sources");
      if (sources) scanPlayerList(out, sources);
      var tracks = readProp(item, "tracks");
      var tn = Math.min(readProp(tracks, "length") || 0, 50);
      for (var j = 0; j < tn; j++) {
        var tr = readProp(tracks, j);
        var tk = String(readProp(tr, "kind") || "captions").toLowerCase();
        if (tk !== "captions" && tk !== "subtitles") continue;
        var tf = readProp(tr, "file") || readProp(tr, "src");
        if (typeof tf === "string") add(out, tf, "s", readProp(tr, "label"), readProp(tr, "language") || readProp(tr, "srclang"));
      }
    }
  }

  function scanPlayers(out) {
    try {
      var jw = W.jwplayer;
      if (typeof jw === "function") {
        for (var i = 0; i < 10; i++) {
          var p = jw(i);
          if (!p || typeof p.getPlaylist !== "function") break;
          scanPlayerList(out, p.getPlaylist());
        }
      }
    } catch (e) {}
    try {
      var vjs = W.videojs;
      var players = vjs && typeof vjs.getPlayers === "function" ? vjs.getPlayers() : null;
      if (players) {
        var keys = W.Object.keys(players);
        for (var k = 0; k < keys.length && k < 10; k++) {
          var pl = players[keys[k]];
          if (!pl) continue;
          if (typeof pl.currentSources === "function") scanPlayerList(out, pl.currentSources());
          var opts = readProp(pl, "options_");
          if (opts) scanPlayerList(out, readProp(opts, "sources"));
        }
      }
    } catch (e) {}
    try {
      var plyr = readProp(W, "player");
      var src = plyr ? readProp(plyr, "source") : null;
      if (src && typeof src === "object") scanPlayerList(out, readProp(src, "sources"));
    } catch (e) {}
  }

  function report(out) {
    if (!out.length) return;
    sentCount += out.length;
    post({ k: "media", f: location.href, ua: navigator.userAgent, items: out });
  }

  function scan(full) {
    if (!document.documentElement || sentCount >= MAX_ITEMS) return;
    var out = [];
    try {
      var list = roots(full);
      for (var i = 0; i < list.length; i++) {
        scanMediaElements(out, list[i]);
        scanLinks(out, list[i]);
        scanDataAttributes(out, list[i]);
      }
      scanScripts(out);
      scanPlayers(out);
    } catch (e) {}
    report(out);
  }

  function schedule(delay, full) {
    if (timer) return;
    timer = setTimeout(function () {
      timer = 0;
      scan(full);
    }, delay);
  }

  function watch() {
    if (observer || !document.documentElement) return;
    observer = new MutationObserver(function (records) {
      for (var i = 0; i < records.length; i++) {
        var r = records[i];
        if (r.type === "attributes" || r.addedNodes.length) { schedule(1500, false); return; }
      }
    });
    observer.observe(document.documentElement, {
      childList: true,
      subtree: true,
      attributes: true,
      attributeFilter: ["src", "data-src", "href"]
    });
    document.addEventListener("loadstart", function () { schedule(500, false); }, true);
  }

  function start() {
    scan(true);
    watch();
    setTimeout(function () { scan(true); }, 2500);
    setTimeout(function () { scan(true); }, 8000);
  }

  return function (m) {
    if (m && m.full) {
      sent.clear();
      scannedScripts = new WeakSet();
      sentCount = 0;
    }
    if (active) { scan(true); return; }
    active = true;
    if (document.readyState === "loading") {
      document.addEventListener("DOMContentLoaded", start, { once: true });
    } else {
      start();
    }
    window.addEventListener("load", function () { scan(true); }, { once: true });
  };
})();
