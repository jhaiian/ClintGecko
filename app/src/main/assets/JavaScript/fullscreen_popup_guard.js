(function() {
    if (window.__clintFsPopupGuardInstalled) return;
    window.__clintFsPopupGuardInstalled = true;

    function isFullscreen() {
        return !!(document.fullscreenElement || document.webkitFullscreenElement || document.mozFullScreenElement || document.msFullscreenElement);
    }

    var nativeOpen = window.open;
    window.open = function() {
        if (isFullscreen()) return null;
        return nativeOpen.apply(window, arguments);
    };

    document.addEventListener('click', function(e) {
        if (!isFullscreen()) return;
        var el = e.target;
        while (el && el !== document) {
            if (el.tagName === 'A' && el.target === '_blank') {
                e.preventDefault();
                e.stopImmediatePropagation();
                return;
            }
            el = el.parentNode;
        }
    }, true);

    document.addEventListener('submit', function(e) {
        if (!isFullscreen()) return;
        var form = e.target;
        if (form && form.target === '_blank') {
            e.preventDefault();
            e.stopImmediatePropagation();
        }
    }, true);
})();
