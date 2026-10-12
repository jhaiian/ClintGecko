(function() {
    if (window.__clintCanvasTouchTracked) return;
    window.__clintCanvasTouchTracked = true;

    function isTouchOverCanvas(x, y) {
        var canvases = document.querySelectorAll('canvas');
        for (var i = 0; i < canvases.length; i++) {
            var rect = canvases[i].getBoundingClientRect();
            if (rect.width > 0 && rect.height > 0 &&
                x >= rect.left && x <= rect.right &&
                y >= rect.top && y <= rect.bottom) {
                return true;
            }
        }
        return false;
    }

    document.addEventListener('touchstart', function(e) {
        var touch = e.touches[0];
        if (touch && isTouchOverCanvas(touch.clientX, touch.clientY)) {
            CanvasTouchBridge.onCanvasTouch(true);
        }
    }, true);

    document.addEventListener('touchend', function() {
        CanvasTouchBridge.onCanvasTouch(false);
    }, true);

    document.addEventListener('touchcancel', function() {
        CanvasTouchBridge.onCanvasTouch(false);
    }, true);
})();
