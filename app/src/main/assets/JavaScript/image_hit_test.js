(function() {
    if (window.__clintImageHitInstalled) return;
    window.__clintImageHitInstalled = true;

    function absolute(url) {
        if (!url) return '';
        try { return new URL(url, document.baseURI).href; } catch (e) { return ''; }
    }

    function visible(el) {
        try {
            var s = getComputedStyle(el);
            return s.visibility !== 'hidden' && s.display !== 'none' && parseFloat(s.opacity) > 0.05;
        } catch (e) { return true; }
    }

    function contains(el, x, y) {
        var r = el.getBoundingClientRect();
        return r.width > 0 && r.height > 0 && x >= r.left && x <= r.right && y >= r.top && y <= r.bottom;
    }

    function largestFromSrcset(srcset) {
        if (!srcset) return '';
        var best = '';
        var bestScore = -1;
        srcset.split(/,\s+/).forEach(function(part) {
            var bits = part.trim().split(/\s+/);
            if (!bits[0]) return;
            var d = bits[1] || '1x';
            var n = parseFloat(d);
            var score = /w$/.test(d) ? n : n * 1000;
            if (!isNaN(score) && score > bestScore) { bestScore = score; best = bits[0]; }
        });
        return absolute(best);
    }

    function imageSource(img) {
        var src = largestFromSrcset(img.getAttribute('srcset'));
        var picture = img.parentElement && img.parentElement.tagName === 'PICTURE' ? img.parentElement : null;
        if (!src && picture) {
            var sources = picture.querySelectorAll('source[srcset]');
            for (var i = 0; i < sources.length && !src; i++) src = largestFromSrcset(sources[i].getAttribute('srcset'));
        }
        if (!src) src = img.currentSrc || img.src || '';
        if (/^data:/i.test(src) && src.length < 512) {
            var lazy = img.getAttribute('data-src') || img.getAttribute('data-lazy-src') || img.getAttribute('data-original') || largestFromSrcset(img.getAttribute('data-srcset'));
            if (lazy) src = absolute(lazy);
        }
        return src;
    }

    function isSpacer(img) {
        return img.complete && img.naturalWidth > 0 && img.naturalWidth <= 2 && img.naturalHeight <= 2;
    }

    function label(el) {
        var text = el.getAttribute && (el.getAttribute('alt') || el.getAttribute('title') || el.getAttribute('aria-label')) || '';
        if (!text && el.closest) {
            var a = el.closest('a');
            if (a) text = a.getAttribute('title') || a.getAttribute('aria-label') || '';
        }
        return (text || '').trim();
    }

    function backgroundUrl(el) {
        var values = [];
        try {
            values.push(getComputedStyle(el).backgroundImage);
            values.push(getComputedStyle(el, '::before').backgroundImage);
            values.push(getComputedStyle(el, '::after').backgroundImage);
        } catch (e) { return ''; }
        for (var i = 0; i < values.length; i++) {
            var m = /url\((['"]?)(.*?)\1\)/.exec(values[i] || '');
            if (m && m[2]) return absolute(m[2]);
        }
        return '';
    }

    function fromElement(el, allowBackground) {
        if (!el || el.nodeType !== 1 || !visible(el)) return null;
        var tag = el.tagName.toUpperCase();
        if (tag === 'IMG') {
            if (isSpacer(el)) return null;
            var src = imageSource(el);
            return src ? { u: src, a: label(el) } : null;
        }
        if (tag === 'PICTURE') {
            var inner = el.querySelector('img');
            return inner ? fromElement(inner, false) : null;
        }
        if (tag === 'VIDEO' && el.poster) return { u: absolute(el.poster), a: label(el) };
        if (tag === 'IMAGE' && el.href && el.href.baseVal) return { u: absolute(el.href.baseVal), a: label(el) };
        if (tag === 'SVG') {
            var box = el.getBoundingClientRect();
            if (box.width >= 64 && box.height >= 64) {
                try {
                    var xml = new XMLSerializer().serializeToString(el);
                    return { u: 'data:image/svg+xml;base64,' + btoa(unescape(encodeURIComponent(xml))), a: label(el) };
                } catch (e) {}
            }
            return null;
        }
        if (tag === 'CANVAS') {
            if (el.width >= 32 && el.height >= 32 && el.width * el.height <= 16777216) {
                try { return { u: el.toDataURL('image/png'), a: label(el) }; } catch (e) {}
            }
            return null;
        }
        if (allowBackground && el !== document.documentElement && el !== document.body) {
            var bg = backgroundUrl(el);
            if (bg) return { u: bg, a: label(el) };
        }
        return null;
    }

    function onText(x, y) {
        var node = null;
        try {
            if (document.caretPositionFromPoint) {
                var pos = document.caretPositionFromPoint(x, y);
                node = pos && pos.offsetNode;
            } else if (document.caretRangeFromPoint) {
                var caret = document.caretRangeFromPoint(x, y);
                node = caret && caret.startContainer;
            }
        } catch (e) {}
        if (!node || node.nodeType !== 3 || !node.nodeValue || !node.nodeValue.trim()) return false;
        var range = document.createRange();
        range.selectNodeContents(node);
        var rects = range.getClientRects();
        for (var i = 0; i < rects.length; i++) {
            var r = rects[i];
            if (x >= r.left && x <= r.right && y >= r.top && y <= r.bottom) return true;
        }
        return false;
    }

    function nearbyImage(start, x, y) {
        var node = start;
        for (var depth = 0; node && node.nodeType === 1 && depth < 4; depth++, node = node.parentElement) {
            if (node === document.body || node === document.documentElement) break;
            var found = node.querySelectorAll('img, video[poster]');
            for (var i = 0; i < found.length && i < 40; i++) {
                if (contains(found[i], x, y)) {
                    var hit = fromElement(found[i], false);
                    if (hit) return hit;
                }
            }
        }
        return null;
    }

    function findImage(x, y, target) {
        if (onText(x, y)) return null;
        var stack = [];
        try { stack = document.elementsFromPoint(x, y) || []; } catch (e) {}
        if (target && target.nodeType === 1 && stack.indexOf(target) < 0) stack.unshift(target);
        var limit = Math.min(stack.length, 30);
        var i;
        for (i = 0; i < limit; i++) {
            var hit = fromElement(stack[i], false);
            if (hit) return hit;
        }
        for (i = 0; i < Math.min(limit, 3); i++) {
            var near = nearbyImage(stack[i], x, y);
            if (near) return near;
        }
        for (i = 0; i < limit; i++) {
            var bg = fromElement(stack[i], true);
            if (bg) return bg;
        }
        return null;
    }

    window.addEventListener('contextmenu', function(e) {
        var image = null;
        var link = '';
        try {
            var target = e.composedPath ? e.composedPath()[0] : e.target;
            image = findImage(e.clientX, e.clientY, target);
            var a = target && target.closest ? target.closest('a[href]') : null;
            if (a) link = a.href || '';
        } catch (err) {}
        try {
            ClintImageHit.report(image ? image.u : '', image ? image.a : '', link);
        } catch (err) {}
    }, true);
})();
