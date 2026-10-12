(function () {
    'use strict';

    function lock(el) {
        try {
            if (el.getAttribute('preload') !== 'none') el.setAttribute('preload', 'none');
        } catch (e) {}
    }

    function lockTree(node) {
        if (!node || node.nodeType !== 1) return;
        if (node.tagName === 'VIDEO' || node.tagName === 'AUDIO') lock(node);
        if (!node.querySelectorAll) return;
        var list = node.querySelectorAll('video,audio');
        for (var i = 0; i < list.length; i++) lock(list[i]);
    }

    try {
        var observer = new MutationObserver(function (records) {
            for (var i = 0; i < records.length; i++) {
                var record = records[i];
                if (record.type === 'attributes') {
                    lockTree(record.target);
                } else {
                    for (var j = 0; j < record.addedNodes.length; j++) lockTree(record.addedNodes[j]);
                }
            }
        });
        observer.observe(document, {
            childList: true,
            subtree: true,
            attributes: true,
            attributeFilter: ['preload']
        });
        lockTree(document.documentElement);
    } catch (e) {}
})();
