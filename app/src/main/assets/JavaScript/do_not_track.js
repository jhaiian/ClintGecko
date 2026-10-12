(function () {
    'use strict';
    try {
        Object.defineProperty(Navigator.prototype, 'doNotTrack', {
            get: function () { return '1'; },
            configurable: true,
            enumerable: true
        });
    } catch (e) {}
})();
