(function(url) {
    var imgs = document.querySelectorAll('img');
    for (var i = 0; i < imgs.length; i++) {
        var img = imgs[i];
        if (img.src === url || img.currentSrc === url) {
            var label = img.alt || img.title || '';
            if (!label) {
                var a = img.closest('a');
                if (a) label = a.title || a.getAttribute('aria-label') || '';
            }
            return label.trim();
        }
    }
    return '';
})('%URL%')
