(function() {
    try {
        Object.defineProperty(navigator, 'maxTouchPoints', {
            get: function() { return 0; },
            configurable: true
        });
    } catch(e) {}

    try {
        Object.defineProperty(navigator, 'msMaxTouchPoints', {
            get: function() { return 0; },
            configurable: true
        });
    } catch(e) {}

    try {
        Object.defineProperty(navigator, 'platform', {
            get: function() { return 'Win32'; },
            configurable: true
        });
    } catch(e) {}

    try { delete window.ontouchstart; } catch(e) {}
    try { delete window.ontouchmove; } catch(e) {}
    try { delete window.ontouchend; } catch(e) {}
    try { delete window.ontouchcancel; } catch(e) {}
    try { delete window.TouchEvent; } catch(e) {}

    try {
        Object.defineProperty(window, 'innerWidth', {
            get: function() { return 1280; },
            configurable: true
        });
    } catch(e) {}

    try {
        Object.defineProperty(window, 'outerWidth', {
            get: function() { return 1280; },
            configurable: true
        });
    } catch(e) {}

    try {
        Object.defineProperty(screen, 'width', {
            get: function() { return 1280; },
            configurable: true
        });
    } catch(e) {}

    try {
        Object.defineProperty(screen, 'availWidth', {
            get: function() { return 1280; },
            configurable: true
        });
    } catch(e) {}

    (function() {
        var origMM = window.matchMedia.bind(window);

        function mkMQL(q, m) {
            return {
                matches: m,
                media: q,
                onchange: null,
                addListener: function() {},
                removeListener: function() {},
                addEventListener: function() {},
                removeEventListener: function() {},
                dispatchEvent: function() { return false; }
            };
        }

        Object.defineProperty(window, 'matchMedia', {
            value: function(q) {
                var lq = (q || '').replace(/\s/g, '').toLowerCase();

                if (lq.indexOf('pointer:coarse') !== -1) return mkMQL(q, false);
                if (lq.indexOf('pointer:fine') !== -1) return mkMQL(q, true);
                if (lq.indexOf('hover:none') !== -1) return mkMQL(q, false);
                if (lq.indexOf('hover:hover') !== -1) return mkMQL(q, true);
                if (lq.indexOf('any-pointer:coarse') !== -1) return mkMQL(q, false);
                if (lq.indexOf('any-pointer:fine') !== -1) return mkMQL(q, true);
                if (lq.indexOf('any-hover:none') !== -1) return mkMQL(q, false);
                if (lq.indexOf('any-hover:hover') !== -1) return mkMQL(q, true);

                var mxW = lq.match(/max-(?:device-)?width:(\d+(?:\.\d+)?)px/);
                if (mxW) return mkMQL(q, 1280 <= parseFloat(mxW[1]));

                var mnW = lq.match(/min-(?:device-)?width:(\d+(?:\.\d+)?)px/);
                if (mnW) return mkMQL(q, 1280 >= parseFloat(mnW[1]));

                return origMM(q);
            },
            configurable: true,
            writable: true
        });
    })();

    var fixVp = function() {
        var m = document.querySelector('meta[name="viewport"]');
        if (m) {
            m.content = 'width=1280';
        } else if (document.head) {
            var n = document.createElement('meta');
            n.name = 'viewport';
            n.content = 'width=1280';
            document.head.insertBefore(n, document.head.firstChild);
        }
    };

    new MutationObserver(fixVp).observe(document.documentElement, {
        childList: true,
        subtree: true
    });

    document.addEventListener('DOMContentLoaded', fixVp);
    if (document.readyState !== 'loading') fixVp();
})();