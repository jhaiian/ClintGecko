(function() {
    if (window.__clintBackgroundPlayback) return;
    window.__clintBackgroundPlayback = true;

    function define(target, name, value) {
        try {
            Object.defineProperty(target, name, {
                get: function() { return value; },
                configurable: true
            });
        } catch (e) {}
    }

    var targets = [];
    try { targets.push(Document.prototype); } catch (e) {}
    targets.push(document);

    targets.forEach(function(target) {
        define(target, 'hidden', false);
        define(target, 'visibilityState', 'visible');
        define(target, 'webkitHidden', false);
        define(target, 'webkitVisibilityState', 'visible');
    });

    var block = function(event) {
        event.stopImmediatePropagation();
    };

    ['visibilitychange', 'webkitvisibilitychange'].forEach(function(name) {
        document.addEventListener(name, block, true);
        window.addEventListener(name, block, true);
    });
})();
