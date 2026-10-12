(function () {
    'use strict';

    var recent = false;
    var timer;

    function mark(event) {
        if (!event.isTrusted) return;
        recent = true;
        clearTimeout(timer);
        timer = setTimeout(function () { recent = false; }, 1500);
    }

    ['pointerdown', 'touchend', 'click', 'keydown'].forEach(function (type) {
        window.addEventListener(type, mark, true);
    });

    function allowed() {
        if (recent) return true;
        try {
            var activation = navigator.userActivation;
            if (activation && activation.isActive) return true;
        } catch (e) {}
        return false;
    }

    try {
        var proto = HTMLMediaElement.prototype;
        var nativePlay = proto.play;
        proto.play = function () {
            if (allowed()) return nativePlay.apply(this, arguments);
            return Promise.reject(new DOMException('Autoplay prevented by Data Saver', 'NotAllowedError'));
        };
    } catch (e) {}

    document.addEventListener('play', function (event) {
        var el = event.target;
        if (!(el instanceof HTMLMediaElement) || allowed()) return;
        try { el.pause(); } catch (e) {}
    }, true);
})();
