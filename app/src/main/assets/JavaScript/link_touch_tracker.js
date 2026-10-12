(function() {
    if (window.__clintLinkTrackerInstalled) return;
    window.__clintLinkTrackerInstalled = true;
    window.__clintLastTouchedLinkText = '';
    document.addEventListener('touchstart', function(e) {
        var el = e.target;
        while (el) {
            if (el.tagName === 'A') {
                window.__clintLastTouchedLinkText = (el.textContent || '').trim().replace(/\s+/g, ' ').substring(0, 200);
                return;
            }
            el = el.parentElement;
        }
    }, true);
})();
