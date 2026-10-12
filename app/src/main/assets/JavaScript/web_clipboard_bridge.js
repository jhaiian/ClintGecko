(function() {
    if (window.__clintClipboardInstalled) return;
    window.__clintClipboardInstalled = true;
    var _seq = 0;
    var _pending = window._ClintClipboardPending || {};
    window._ClintClipboardPending = _pending;

    function requestRead() {
        var id = 'c' + String(++_seq);
        return new Promise(function(resolve, reject) {
            _pending[id] = function(result) {
                delete _pending[id];
                if (result === 'granted') {
                    resolve(ClintClipboardBridge.readClipboardText());
                } else {
                    reject(new DOMException('Clipboard read permission denied', 'NotAllowedError'));
                }
            };
            ClintClipboardBridge.requestPermission(id, String(window.location.hostname || ''));
        });
    }

    var prevResolve = window._ClintResolvePermission;
    window._ClintResolvePermission = function(id, result) {
        var cb = _pending[String(id)];
        if (cb) { cb(result); return; }
        if (typeof prevResolve === 'function') prevResolve(id, result);
    };

    var clipboardPolyfill = {
        readText: requestRead,
        writeText: function(text) {
            return new Promise(function(resolve) {
                ClintClipboardBridge.writeClipboardText(String(text || ''));
                resolve();
            });
        }
    };

    try {
        if (!navigator.clipboard) {
            Object.defineProperty(navigator, 'clipboard', {
                value: clipboardPolyfill,
                configurable: true
            });
        } else {
            var orig = navigator.clipboard;
            orig.readText = clipboardPolyfill.readText;
            if (!orig.writeText) orig.writeText = clipboardPolyfill.writeText;
        }
    } catch (e) {}
})();
