(function() {
    if (window.__clintTracked) return;
    window.__clintTracked = true;

    var root = document.scrollingElement || document.documentElement;

    function blocksPull(el) {
        while (el && el.nodeType === 1 && el !== root && el !== document.body) {
            var style = window.getComputedStyle(el);
            var ta = style.touchAction || 'auto';
            if (ta !== 'auto' && ta !== 'manipulation' && !/pan-y|pan-down/.test(ta)) return true;
            var oy = style.overflowY;
            if ((oy === 'auto' || oy === 'scroll' || oy === 'overlay') && el.scrollHeight > el.clientHeight + 1) {
                if (el.scrollTop > 0) return true;
                if (/\b(y|block|both)\b/.test(style.scrollSnapType || '')) return true;
                var ob = style.overscrollBehaviorY || '';
                if (ob === 'contain' || ob === 'none') return true;
            }
            el = el.parentElement;
        }
        return false;
    }

    document.addEventListener('touchstart', function(e) {
        if (!e.touches || e.touches.length !== 1) return;
        var t = e.target;
        NestedScrollBridge.onNestedScroll(blocksPull(t && t.nodeType === 1 ? t : t && t.parentElement));
    }, { capture: true, passive: true });

    document.addEventListener('scroll', function(e) {
        var t = e.target;
        var isRoot = !t || t === document || t === document.documentElement || t === document.body;
        var nested = !isRoot && (t.scrollTop > 0 || t.scrollLeft > 0);
        NestedScrollBridge.onNestedScroll(nested);
    }, true);
})();
