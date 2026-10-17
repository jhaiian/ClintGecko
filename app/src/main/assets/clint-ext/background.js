(function () {
  var NATIVE = "clintBg";
  var cfg = { media: false, dnt: false, ds: null };
  var reg = null;
  var port = null;
  var EMPTY_JS = "data:application/javascript;charset=utf-8,";
  var EMPTY_CSS = "data:text/css;charset=utf-8,";
  var EMPTY_HTML = "data:text/html;charset=utf-8,";
  var FRESH_SECONDS = 604800;
  var typeMap = {
    main_frame: "document", sub_frame: "sub_frame", stylesheet: "stylesheet", script: "script",
    image: "image", imageset: "image", font: "font", object: "object", object_subrequest: "object",
    xmlhttprequest: "xhr", xhr: "xhr", fetch: "xhr", ping: "xhr", beacon: "xhr", csp_report: "other",
    media: "media", websocket: "websocket", speculative: "other", web_manifest: "other", other: "other"
  };
  var observed = { media: 1, xmlhttprequest: 1, xhr: 1, fetch: 1, other: 1, object: 1, object_subrequest: 1, sub_frame: 1 };
  var lightTypes = { main_frame: 1, sub_frame: 1, image: 1, imageset: 1, media: 1 };
  var staticTypes = { image: 1, imageset: 1, font: 1 };
  var secondLevel = { co: 1, com: 1, org: 1, net: 1, gov: 1, edu: 1, ac: 1, or: 1, ne: 1, go: 1, gob: 1, mil: 1 };
  var iconFontHints = ["icon", "awesome", "glyph", "symbol", "material", "fa-", "fa5", "fa6", "dashicons", "bootstrap", "ionicons", "feather", "codicon"];
  var audioPath = /\.(mp3|aac|m4a|oga|ogg|opus|wav|flac|weba)$/;
  var videoPath = /\.(mp4|m4v|webm|mkv|mov|ogv|3gp|f4v|m3u8|mpd|m4s|ts)$/;
  var allowedFrameHosts = [
    "hcaptcha.com", "recaptcha.net", "challenges.cloudflare.com", "arkoselabs.com", "funcaptcha.com", "geetest.com",
    "stripe.com", "stripe.network", "paypal.com", "braintreegateway.com", "adyen.com", "checkout.com", "pay.google.com",
    "accounts.google.com", "appleid.apple.com", "login.microsoftonline.com", "login.live.com", "auth0.com", "okta.com",
    "google.com/maps", "openstreetmap.org", "spotify.com", "soundcloud.com", "bandcamp.com"
  ];
  var allowedFramePath = ["/recaptcha/", "/turnstile/", "/maps/embed", "/oauth", "/signin", "/login", "/sso/"];
  var allowedFrameHostPrefix = ["accounts.", "login.", "auth.", "sso.", "signin.", "secure.", "checkout.", "pay."];
  var adFrameHints = [
    "doubleclick", "googlesyndication", "googleadservices", "adservice", "adsystem", "adnxs", "taboola", "outbrain",
    "popads", "popcash", "propeller", "exoclick", "juicyads", "trafficjunky", "adsterra", "clickadu", "hilltopads",
    "revcontent", "mgid", "criteo", "pubmatic", "rubiconproject", "amazon-adsystem", "adform", "smartadserver", "openx"
  ];
  var playerHostHints = [
    "embed", "player", "stream", "vidsrc", "vidplay", "vidcloud", "vidmoly", "vidhide", "vidguard", "vidlink", "vidfast",
    "videasy", "filemoon", "streamwish", "streamtape", "doodstream", "dood.", "mixdrop", "voe.", "upstream", "primesrc",
    "2embed", "multiembed", "autoembed", "superembed", "moviesapi", "rabbitstream", "megacloud", "youtube.com",
    "youtube-nocookie.com", "youtu.be", "vimeo.com", "dailymotion.com", "twitch.tv", "streamable.com", "jwplatform.com",
    "jwpsrv.com", "brightcove", "wistia"
  ];
  var playerPathHints = ["/embed", "/player", "/e/", "/v/", "/play", "/watch", "/stream", "/video", "/iframe", "/tv/", "/movie/"];

  function send(msg) {
    try { return browser.runtime.sendNativeMessage(NATIVE, msg); } catch (e) { return Promise.reject(e); }
  }

  function pageUrlOf(d) {
    if (d.frameAncestors && d.frameAncestors.length) return d.frameAncestors[d.frameAncestors.length - 1].url;
    return d.documentUrl || d.originUrl || "";
  }

  function isHttp(u) {
    return u.indexOf("http://") === 0 || u.indexOf("https://") === 0;
  }

  function isWeb(u) {
    return isHttp(u) || u.indexOf("ws://") === 0 || u.indexOf("wss://") === 0;
  }

  function parse(u) {
    try { return new URL(u); } catch (e) { return null; }
  }

  function hostOf(u) {
    var p = parse(u);
    return p ? p.hostname.toLowerCase() : "";
  }

  function siteOf(h) {
    var p = h.split(".");
    if (p.length <= 2) return h;
    var tld = p[p.length - 1];
    var sld = p[p.length - 2];
    if (tld.length === 2 && secondLevel[sld]) return p.slice(-3).join(".");
    return p.slice(-2).join(".");
  }

  function skipped(ds, host) {
    if (!host || !ds.skip) return false;
    for (var i = 0; i < ds.skip.length; i++) {
      var s = ds.skip[i];
      if (host === s || host.endsWith("." + s)) return true;
    }
    return false;
  }

  function containsAny(text, list) {
    for (var i = 0; i < list.length; i++) {
      if (text.indexOf(list[i]) !== -1) return true;
    }
    return false;
  }

  function hostAllowed(host, path) {
    for (var i = 0; i < allowedFrameHosts.length; i++) {
      var entry = allowedFrameHosts[i];
      var slash = entry.indexOf("/");
      if (slash === -1) {
        if (host === entry || host.endsWith("." + entry)) return true;
      } else {
        var h = entry.slice(0, slash);
        if ((host === h || host.endsWith("." + h)) && path.indexOf(entry.slice(slash)) === 0) return true;
      }
    }
    for (var j = 0; j < allowedFrameHostPrefix.length; j++) {
      if (host.indexOf(allowedFrameHostPrefix[j]) === 0) return true;
    }
    return containsAny(path, allowedFramePath);
  }

  function frameBlocked(d, url, pageHost) {
    if (d.parentFrameId !== 0 || !pageHost) return false;
    var host = url.hostname.toLowerCase();
    if (siteOf(host) === siteOf(pageHost)) return false;
    var path = url.pathname.toLowerCase();
    if (hostAllowed(host, path)) return false;
    if (containsAny(host, adFrameHints)) return true;
    if (containsAny(host, playerHostHints) || containsAny(path, playerPathHints)) return false;
    return true;
  }

  function fontBlocked(url) {
    var path = url.pathname.toLowerCase();
    return !containsAny(path, iconFontHints);
  }

  function decide(d) {
    var ds = cfg.ds;
    if (!ds || !ds.on || d.tabId < 0 || d.type === "main_frame" || !isHttp(d.url)) return {};
    var pageHost = hostOf(pageUrlOf(d));
    if (skipped(ds, pageHost)) return {};
    var url = parse(d.url);
    if (!url) return {};
    var path = url.pathname.toLowerCase();
    switch (d.type) {
      case "image":
      case "imageset":
        if (ds.images) return { cancel: true };
        break;
      case "font":
        if (ds.fonts && fontBlocked(url)) return { cancel: true };
        break;
      case "media":
        if (ds.video && !audioPath.test(path)) return { cancel: true };
        break;
      case "xmlhttprequest":
      case "xhr":
      case "fetch":
      case "other":
        if (ds.video && videoPath.test(path)) return { cancel: true };
        break;
      case "script":
        if (ds.scripts) return { redirectUrl: EMPTY_JS };
        break;
      case "stylesheet":
        if (ds.css) return { redirectUrl: EMPTY_CSS };
        break;
      case "sub_frame":
        if (ds.frames && frameBlocked(d, url, pageHost)) return { redirectUrl: EMPTY_HTML };
        break;
    }
    return {};
  }

  function hasHeader(headers, name) {
    var n = name.toLowerCase();
    for (var i = 0; i < headers.length; i++) {
      if (headers[i].name.toLowerCase() === n) return true;
    }
    return false;
  }

  function wantsSaveData(d) {
    var ds = cfg.ds;
    if (!ds || !ds.on || !ds.header || d.tabId < 0 || !lightTypes[d.type] || !isHttp(d.url)) return false;
    var host = d.type === "main_frame" ? hostOf(d.url) : hostOf(pageUrlOf(d));
    return !skipped(ds, host);
  }

  function extendFreshness(d) {
    var ds = cfg.ds;
    if (!ds || !ds.on || !ds.cache || d.tabId < 0 || !staticTypes[d.type] || d.method !== "GET" || d.statusCode !== 200) return {};
    if (skipped(ds, hostOf(pageUrlOf(d)))) return {};
    var headers = d.responseHeaders || [];
    var cc = "";
    var out = [];
    for (var i = 0; i < headers.length; i++) {
      var h = headers[i];
      var n = h.name.toLowerCase();
      var v = h.value || "";
      if (n === "cache-control") { cc += v + ","; continue; }
      if (n === "expires") continue;
      if (n === "pragma" && /no-cache/i.test(v)) return {};
      if (n === "vary" && v.trim() === "*") return {};
      out.push(h);
    }
    if (/no-store|no-cache|private/i.test(cc)) return {};
    var m = /max-age=(\d+)/i.exec(cc);
    if (m && parseInt(m[1], 10) >= FRESH_SECONDS) return {};
    out.push({ name: "Cache-Control", value: "public, max-age=" + FRESH_SECONDS });
    return { responseHeaders: out };
  }

  async function registerBundle(code) {
    if (reg) { try { await reg.unregister(); } catch (e) {} reg = null; }
    try {
      reg = await browser.contentScripts.register({
        matches: ["<all_urls>"],
        js: [{ code: code }],
        runAt: "document_start",
        allFrames: true,
        matchAboutBlank: true
      });
    } catch (e) {}
  }

  async function cookiesFor(m) {
    var out = "";
    try {
      var q = { url: m.url };
      if (m.storeId) q.storeId = m.storeId;
      var list = await browser.cookies.getAll(q);
      out = list.map(function (c) { return c.name + "=" + c.value; }).join("; ");
    } catch (e) {}
    port.postMessage({ k: "cookiesRes", id: m.id, cookie: out });
  }

  function ckUrl(c) {
    return (c.secure ? "https://" : "http://") + c.domain.replace(/^\./, "") + c.path;
  }

  async function ckGetAll(url, storeId) {
    var q = { url: url };
    if (storeId) q.storeId = storeId;
    try {
      q.partitionKey = {};
      return await browser.cookies.getAll(q);
    } catch (e) {
      delete q.partitionKey;
      return await browser.cookies.getAll(q);
    }
  }

  async function ckList(m) {
    var list = await ckGetAll(m.url, m.storeId);
    return list.map(function (c) {
      return {
        name: c.name,
        value: c.value,
        domain: c.domain,
        hostOnly: !!c.hostOnly,
        path: c.path,
        secure: !!c.secure,
        httpOnly: !!c.httpOnly,
        exp: c.expirationDate || 0
      };
    });
  }

  async function ckRemove(m) {
    var list = await ckGetAll(m.url, m.storeId);
    var removed = 0;
    var failed = 0;
    for (var c of list) {
      try {
        var d = { url: ckUrl(c), name: c.name };
        if (c.storeId) d.storeId = c.storeId;
        if (c.partitionKey) d.partitionKey = c.partitionKey;
        if (c.firstPartyDomain) d.firstPartyDomain = c.firstPartyDomain;
        var r = await browser.cookies.remove(d);
        if (r) removed++; else failed++;
      } catch (e) { failed++; }
    }
    return { removed: removed, failed: failed };
  }

  async function ckSet(m) {
    var ok = 0;
    var failed = 0;
    var items = m.cookies || [];
    for (var c of items) {
      try {
        var d = {
          url: c.url,
          name: c.name,
          value: c.value,
          path: c.path || "/",
          secure: !!c.secure,
          httpOnly: !!c.httpOnly
        };
        if (c.domain) d.domain = c.domain;
        if (c.exp > 0) d.expirationDate = c.exp;
        if (m.storeId) d.storeId = m.storeId;
        var r = await browser.cookies.set(d);
        if (r) ok++; else failed++;
      } catch (e) { failed++; }
    }
    return { ok: ok, failed: failed };
  }

  async function cookieOp(m) {
    var res = null;
    try {
      if (m.op === "list") res = await ckList(m);
      else if (m.op === "remove") res = await ckRemove(m);
      else if (m.op === "set") res = await ckSet(m);
    } catch (e) {}
    port.postMessage({ k: "ckRes", id: m.id, res: res });
  }

  function onNative(m) {
    if (!m) return;
    if (m.k === "bundle") {
      registerBundle(m.code).then(function () { port.postMessage({ k: "bundleOk", seq: m.seq }); });
    } else if (m.k === "cfg") {
      if (m.cfg) cfg = m.cfg;
    } else if (m.k === "cookies") {
      cookiesFor(m);
    } else if (m.k === "ck") {
      cookieOp(m);
    }
  }

  var attempts = 0;
  function connect() {
    try {
      var p = browser.runtime.connectNative(NATIVE);
      port = p;
      p.onMessage.addListener(onNative);
      p.onDisconnect.addListener(function () {
        if (port === p) port = null;
        if (attempts++ < 40) setTimeout(connect, 500);
      });
      p.postMessage({ k: "ready" });
    } catch (e) {
      if (attempts++ < 40) setTimeout(connect, 500);
    }
  }
  connect();

  browser.runtime.onMessage.addListener(function (msg, sender) {
    if (msg && msg.k === "who" && sender && sender.tab) {
      send({ k: "tab", n: msg.n, tabId: sender.tab.id, storeId: sender.tab.cookieStoreId || "" });
    }
  });

  browser.webRequest.onBeforeRequest.addListener(decide, { urls: ["<all_urls>"] }, ["blocking"]);

  browser.webRequest.onBeforeSendHeaders.addListener(function (d) {
    var dnt = cfg.dnt && isWeb(d.url);
    var save = wantsSaveData(d);
    if (!dnt && !save) return {};
    var headers = d.requestHeaders || [];
    var changed = false;
    if (dnt && !hasHeader(headers, "DNT")) { headers.push({ name: "DNT", value: "1" }); changed = true; }
    if (save && !hasHeader(headers, "Save-Data")) { headers.push({ name: "Save-Data", value: "on" }); changed = true; }
    return changed ? { requestHeaders: headers } : {};
  }, { urls: ["<all_urls>"] }, ["blocking", "requestHeaders"]);

  browser.webRequest.onHeadersReceived.addListener(extendFreshness, { urls: ["<all_urls>"] }, ["blocking", "responseHeaders"]);

  browser.webRequest.onSendHeaders.addListener(function (d) {
    if (!cfg.media || !observed[d.type] || !isWeb(d.url) || d.url.indexOf("ws") === 0) return;
    var headers = {};
    (d.requestHeaders || []).forEach(function (h) {
      var n = h.name.toLowerCase();
      if (n !== "cookie") headers[h.name] = h.value;
    });
    send({
      k: "obs", u: d.url, mt: d.method, t: typeMap[d.type] || "other", tab: d.tabId,
      h: headers, page: pageUrlOf(d)
    });
  }, { urls: ["<all_urls>"] }, ["requestHeaders"]);
})();
