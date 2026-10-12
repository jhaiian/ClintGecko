(function() {
    if (window.__clintBottomNavDetected !== undefined) return;
    window.__clintBottomNavDetected = false;

    function check() {
        if (window.__clintBottomNavDetected) return;
        var vh = window.innerHeight;
        var vw = window.innerWidth;
        var minTop = vh * 0.55;
        var maxHeight = vh * 0.25;
        var minWidth = vw * 0.45;
        var els = document.querySelectorAll('*');
        for (var i = 0; i < els.length; i++) {
            var el = els[i];
            if (!el.offsetParent && el.tagName !== 'BODY' && el.tagName !== 'HTML') continue;
            var s = window.getComputedStyle(el);
            if (s.position !== 'fixed' && s.position !== 'sticky') continue;
            if (s.display === 'none' || s.visibility === 'hidden') continue;
            var r = el.getBoundingClientRect();
            if (r.width >= minWidth && r.height > 0 && r.height <= maxHeight && r.top >= minTop) {
                window.__clintBottomNavDetected = true;
                BottomNavBridge.onBottomNavDetected(true);
                return;
            }
        }
    }

    var debounceTimer = null;
    function debounceCheck() {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(check, 300);
    }

    setTimeout(check, 600);
    setTimeout(check, 2000);

    var observer = new MutationObserver(function() {
        if (!window.__clintBottomNavDetected) debounceCheck();
    });

    function startObserver() {
        var target = document.body || document.documentElement;
        if (!target) return;
        observer.observe(target, { childList: true, subtree: true });
        setTimeout(function() { observer.disconnect(); }, 10000);
    }

    if (document.body) {
        startObserver();
    } else {
        document.addEventListener('DOMContentLoaded', startObserver);
    }
})();
