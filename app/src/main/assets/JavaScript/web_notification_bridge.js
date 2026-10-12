(function() {
    if (window.__clintNotifInstalled) return;
    window.__clintNotifInstalled = true;
    var _seq = 0;
    var _pending = {};
    function ClintNotification(title, options) {
        if (!(this instanceof ClintNotification)) return;
        options = options || {};
        ClintNotificationBridge.postNotification(
            String(title || ''),
            String(options.body || ''),
            String(options.tag || ''),
            String(window.location.hostname || '')
        );
    }
    ClintNotification.prototype.close = function() {};
    Object.defineProperty(ClintNotification, 'permission', {
        get: function() {
            return ClintNotificationBridge.getPermissionState(String(window.location.hostname || ''));
        },
        configurable: true
    });
    ClintNotification.requestPermission = function(callback) {
        var id = String(++_seq);
        return new Promise(function(resolve) {
            _pending[id] = function(result) {
                delete _pending[id];
                if (typeof callback === 'function') callback(result);
                resolve(result);
            };
            ClintNotificationBridge.requestPermission(id, String(window.location.hostname || ''));
        });
    };
    window._ClintResolvePermission = function(id, result) {
        var cb = _pending[String(id)];
        if (cb) cb(result);
    };
    window.Notification = ClintNotification;
})();
