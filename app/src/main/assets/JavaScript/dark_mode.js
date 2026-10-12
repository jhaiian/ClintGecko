(function() {
    var id = '__clint_dark_mode';
    var existing = document.getElementById(id);
    if (existing) { existing.remove(); return; }
    var s = document.createElement('style');
    s.id = id;
    s.textContent = 'html { filter: invert(100%) hue-rotate(180deg) !important; background: #fff !important; } img, video, canvas, picture, svg, iframe { filter: invert(100%) hue-rotate(180deg) !important; }';
    (document.head || document.documentElement).appendChild(s);
})();
